import os
from PIL import Image

res_dir = "/home/ajaydev/Desktop/MissIn_App/app/src/main/res"
logo_path = "/home/ajaydev/Downloads/missIn_Logo.png"

# 1. Asset Purge
targets = ["ic_launcher.png", "ic_launcher_round.png", "ic_launcher_foreground.png"]
for root, dirs, files in os.walk(res_dir):
    for f in files:
        if f in targets and root.split('/')[-1].startswith("mipmap-"):
            path = os.path.join(root, f)
            print(f"Deleting {path}")
            os.remove(path)

# 2. Asset Generation
logo = Image.open(logo_path).convert("RGBA")

# Ensure transparency logic
def process_logo_transparency(img):
    # The requirement: "Only the white pin logo should contain color. 100% transparent backgrounds."
    # If the image has a black or colored background, we want to make it transparent.
    corner_pixel = img.getpixel((0, 0))
    if corner_pixel[3] > 0: # Not fully transparent
        print(f"Image has opaque background: {corner_pixel}, making it transparent...")
        target_color = (corner_pixel[0], corner_pixel[1], corner_pixel[2])
        data = img.getdata()
        new_data = []
        for item in data:
            if abs(item[0]-target_color[0]) < 20 and abs(item[1]-target_color[1]) < 20 and abs(item[2]-target_color[2]) < 20:
                new_data.append((255, 255, 255, 0))
            else:
                new_data.append(item)
        img.putdata(new_data)
    else:
        print("Image already has transparent background.")
    return img

logo = process_logo_transparency(logo)

def make_icon(size, out_path):
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    # 60% of canvas
    logo_size = int(size * 0.6)
    
    w, h = logo.size
    aspect = w / h
    if aspect > 1:
        new_w = logo_size
        new_h = int(logo_size / aspect)
    else:
        new_h = logo_size
        new_w = int(logo_size * aspect)
        
    logo_resized = logo.resize((new_w, new_h), Image.Resampling.LANCZOS)
    
    x = (size - new_w) // 2
    y = (size - new_h) // 2
    canvas.paste(logo_resized, (x, y), logo_resized)
    canvas.save(out_path, "PNG")

densities = {
    "mipmap-mdpi": 108,
    "mipmap-hdpi": 162,
    "mipmap-xhdpi": 216,
    "mipmap-xxhdpi": 324,
    "mipmap-xxxhdpi": 432
}

for folder, size in densities.items():
    folder_path = os.path.join(res_dir, folder)
    os.makedirs(folder_path, exist_ok=True)
    out_file = os.path.join(folder_path, "ic_launcher_foreground.png")
    make_icon(size, out_file)
    print(f"Generated {out_file}")

# Splash icon
drawable_dir = os.path.join(res_dir, "drawable")
os.makedirs(drawable_dir, exist_ok=True)
out_splash = os.path.join(drawable_dir, "ic_splash_icon.png")
make_icon(288, out_splash)
print(f"Generated {out_splash}")
