import os
import shutil
import re
source_dir = r'C:\Users\K.Pavan Kumar\.gemini\antigravity\brain\0066bcb6-9ecd-4caf-9011-37b65fb7490a'
dest_dir = r'D:\gdg\JFSD_Proj\frontend\public'
os.makedirs(dest_dir, exist_ok=True)
for filename in os.listdir(source_dir):
    if filename.endswith('.png'):
        match = re.search(r'^(.*?)_\d+\.png$', filename)
        if match:
            clean_name = match.group(1) + '.png'
            src_path = os.path.join(source_dir, filename)
            dst_path = os.path.join(dest_dir, clean_name)
            shutil.copy2(src_path, dst_path)
            print(f'Copied {filename} to {clean_name}')
