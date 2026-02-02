import os
from PIL import Image, ImageSequence

TARGET_WIDTH = 512
TARGET_HEIGHT = 366
DIRECTORY = r'a:\Software Dev\Platisa\app\src\main\res\drawable'
FILES = [
    'celebration_4.gif', 
    'celebration_dance.gif', 
    'macka.gif', 
    'celebration_3.gif', 
    'fall.gif', 
    'aquarium.gif', 
    'fireworks.gif'
]

def resize_and_crop(img, target_width, target_height):
    # Calculate aspect ratios
    target_ratio = target_width / target_height
    img_ratio = img.width / img.height

    if img_ratio > target_ratio:
        # Image is wider than target: resize by height, then crop width
        new_height = target_height
        new_width = int(new_height * img_ratio)
        img = img.resize((new_width, new_height), Image.Resampling.LANCZOS)
        
        # Center crop
        left = (new_width - target_width) // 2
        img = img.crop((left, 0, left + target_width, target_height))
    else:
        # Image is taller/equal: resize by width, then crop height
        new_width = target_width
        new_height = int(new_width / img_ratio)
        img = img.resize((new_width, new_height), Image.Resampling.LANCZOS)
        
        # Center crop
        top = (new_height - target_height) // 2
        img = img.crop((0, top, target_width, top + target_height))
        
    return img

def process_gif(filename):
    filepath = os.path.join(DIRECTORY, filename)
    if not os.path.exists(filepath):
        print(f"Skipping {filename}: Not found")
        return

    print(f"Processing {filename}...")
    with Image.open(filepath) as im:
        frames = []
        duration = im.info.get('duration', 100)
        loop = im.info.get('loop', 0)
        
        for frame in ImageSequence.Iterator(im):
            # Convert to RGBA for high quality resizing
            frame = frame.convert('RGBA')
            processed_frame = resize_and_crop(frame, TARGET_WIDTH, TARGET_HEIGHT)
            
            # Convert back to P mode with palette for GIF
            # We use the quantize method to generate a good palette for each frame? 
            # Or better: just let save handle it (it might dither).
            # For simplicity and robust GIF saving, converting to RGB then letting P mode handle it is usually okay,
            # but sometimes creates flicker. 
            # Trying 'P' directly:
            frames.append(processed_frame)

        # Save
        if frames:
            # Note: Saving as GIF from RGBA frames forces quantization
            frames[0].save(
                filepath,
                save_all=True,
                append_images=frames[1:],
                optimize=False, # Optimization sometimes breaks loops in PIL
                duration=duration,
                loop=loop
            )
            print(f"Saved {filename} ({TARGET_WIDTH}x{TARGET_HEIGHT})")

if __name__ == "__main__":
    for f in FILES:
        try:
            process_gif(f)
        except Exception as e:
            print(f"Error processing {f}: {e}")
