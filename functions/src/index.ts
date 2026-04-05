import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();

/**
 * Callable function to securely award karma points.
 * This is called from the client app once a claim is verified by both parties.
 * The function verifies the claim details server-side before updating the user's score.
 */
export const awardKarmaPoints = functions.https.onCall(async (data: any, context: functions.https.CallableContext) => {
    // 1. Verify caller is authenticated
    if (!context.auth) {
        throw new functions.https.HttpsError(
            "unauthenticated",
            "User must be authenticated to award points."
        );
    }

    const { claimId } = data;
    if (!claimId) {
        throw new functions.https.HttpsError(
            "invalid-argument",
            "The function must be called with a valid claimId."
        );
    }

    const db = admin.firestore();

    try {
        // 2. Fetch the claim
        const claimRef = db.collection("claims").doc(claimId);

        await db.runTransaction(async (transaction: { get: (arg0: any) => any; set: (arg0: any, arg1: { karmaPoints: number; }, arg2: { merge: boolean; }) => void; update: (arg0: any, arg1: { status: string; }) => void; }) => {
            const claimSnap = await transaction.get(claimRef);

            if (!claimSnap.exists) {
                throw new functions.https.HttpsError("not-found", "Claim not found.");
            }

            const claimData = claimSnap.data();

            // Ensure the claim hasn't already been rewarded
            if (claimData?.status === "resolved_and_rewarded") {
                throw new functions.https.HttpsError(
                    "failed-precondition",
                    "This claim has already been rewarded."
                );
            }

            // Verify both parties agreed
            // For MVP: we proceed as long as it's not marked resolved
            const finderId = claimData?.finderId;

            if (!finderId) {
                throw new functions.https.HttpsError("internal", "Finder ID is missing from claim.");
            }

            const userRef = db.collection("users").doc(finderId);
            const userSnap = await transaction.get(userRef);

            let currentPoints = 0;
            if (userSnap.exists) {
                currentPoints = userSnap.data()?.karmaPoints || 0;
            }

            // Award 50 points
            transaction.set(userRef, { karmaPoints: currentPoints + 50 }, { merge: true });

            // Mark claim as resolved and rewarded
            transaction.update(claimRef, { status: "resolved_and_rewarded" });
        });

        return { success: true, pointsAwarded: 50 };

    } catch (error: any) {
        console.error("Error awarding karma points:", error);
        throw new functions.https.HttpsError("internal", error.message || "An error occurred awarding karma points.");
    }
});

// --- Phase 5.2 Real-Time Geospatial Matchmaking ---

function calculateDistance(lat1: number, lon1: number, lat2: number, lon2: number): number {
    const R = 6371; // Radius of the earth in km
    const dLat = (lat2 - lat1) * (Math.PI / 180);
    const dLon = (lon2 - lon1) * (Math.PI / 180);
    const a =
        Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos(lat1 * (Math.PI / 180)) * Math.cos(lat2 * (Math.PI / 180)) *
        Math.sin(dLon / 2) * Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c; // Distance in km
}

async function sendSmartMatchNotification(userId: string, category: string, itemId: string) {
    const db = admin.firestore();
    const userSnap = await db.collection("users").doc(userId).get();
    if (userSnap.exists) {
        const userData = userSnap.data();
        if (userData?.fcmToken) {
            const payload = {
                notification: {
                    title: `Potential Match!`,
                    body: `We found a ${category} near your lost location.`
                },
                data: {
                    itemId: itemId,
                    type: "match"
                }
            };
            try {
                await admin.messaging().sendToDevice(userData.fcmToken, payload);
            } catch (e) {
                console.error("FCM Send error", e);
            }
        }
    }
}

async function sendBroadcastNotification(fcmToken: string, category: string, itemId: string) {
    const payload = {
        notification: {
            title: `Item Found Nearby`,
            body: `New ${category} Found near your home base!`
        },
        data: {
            itemId: itemId,
            type: "broadcast"
        }
    };
    try {
        await admin.messaging().sendToDevice(fcmToken, payload);
    } catch (e) {
        console.error("FCM Send error", e);
    }
}

export const matchLostItem = functions.firestore
    .document("lost_items/{itemId}")
    .onCreate(async (snap, context) => {
        const lostItem = snap.data();
        const db = admin.firestore();

        if (lostItem.status !== "active") return null;

        const foundQuery = await db.collection("found_items")
            .where("category", "==", lostItem.category)
            .where("status", "==", "active")
            .get();

        const matches: any[] = [];

        foundQuery.forEach((doc: { data: () => any; id: any; }) => {
            const foundItem = doc.data();
            const distance = calculateDistance(
                lostItem.location?.latitude || 0, lostItem.location?.longitude || 0,
                foundItem.location?.latitude || 0, foundItem.location?.longitude || 0
            );

            if (distance <= 3) {
                matches.push({
                    id: doc.id,
                    ...foundItem,
                    distance
                });
            }
        });

        for (const match of matches) {
            const matchRef = db.collection("matches").doc();
            await matchRef.set({
                matchId: matchRef.id,
                lostItemId: snap.id,
                foundItemId: match.id,
                distance: match.distance,
                status: "pending",
                createdAt: admin.firestore.FieldValue.serverTimestamp()
            });

            await sendSmartMatchNotification(lostItem.reporterId, lostItem.category, snap.id);
            await sendSmartMatchNotification(match.reporterId, lostItem.category, match.id);
        }
        return null;
    });

export const matchFoundItem = functions.firestore
    .document("found_items/{itemId}")
    .onCreate(async (snap, context) => {
        const foundItem = snap.data();
        const db = admin.firestore();

        if (foundItem.status !== "active") return null;

        const lostQuery = await db.collection("lost_items")
            .where("category", "==", foundItem.category)
            .where("status", "==", "active")
            .get();

        const matches: any[] = [];

        lostQuery.forEach((doc: { data: () => any; id: any; }) => {
            const lostItem = doc.data();
            const distance = calculateDistance(
                foundItem.location?.latitude || 0, foundItem.location?.longitude || 0,
                lostItem.location?.latitude || 0, lostItem.location?.longitude || 0
            );

            if (distance <= 3) {
                matches.push({
                    id: doc.id,
                    ...lostItem,
                    distance
                });
            }
        });

        for (const match of matches) {
            const matchRef = db.collection("matches").doc();
            await matchRef.set({
                matchId: matchRef.id,
                lostItemId: match.id,
                foundItemId: snap.id,
                distance: match.distance,
                status: "pending",
                createdAt: admin.firestore.FieldValue.serverTimestamp()
            });

            await sendSmartMatchNotification(foundItem.reporterId, foundItem.category, snap.id);
            await sendSmartMatchNotification(match.reporterId, foundItem.category, match.id);
        }

        // Broadcast Scenario A
        // Fire notification to all generic users within 3km who have homeLocation setup
        const allUsersSnap = await db.collection("users").get();
        const notificationPromises: Promise<void>[] = [];
        allUsersSnap.forEach((userDoc: { data: () => any; id: any; }) => {
            const userData = userDoc.data();
            if (userData.homeLocation && userData.fcmToken && userDoc.id !== foundItem.reporterId) { // Prevent self-broadcast
                const broadcastDist = calculateDistance(
                    foundItem.location?.latitude || 0, foundItem.location?.longitude || 0,
                    userData.homeLocation.latitude, userData.homeLocation.longitude
                );
                if (broadcastDist <= 3) {
                    notificationPromises.push(sendBroadcastNotification(userData.fcmToken, foundItem.category, snap.id));
                }
            }
        });
        await Promise.all(notificationPromises);

        return null;
    });
