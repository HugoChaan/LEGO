import os
import sys
import json
import subprocess

update_map_files = [
    "FURenderKit.framework/FURenderKit",
    "FUAvatarFactory.framework/FUAvatarFactory",
    "FUAvatarFactory.aar",
    "FUAvatarFactory-sources.jar",
    "version.gradle",
    "ai_face_processor_e47_s1.bundle",
    "ai_human_processor.bundle",
    "controller_cpp.bundle",
    "controller_config.bundle",
    "face_components_fit.json",
    "face_feature_fit.json",
    "edit_color_list.json",
    "pta_core.bin",
    "pta_server_dl_lite_g.bin",
    "pta_server_g_mg.bin",
    "/light/",
    "/body/",
    "/head/",
    "/brow/",
    "/face_components/"
]

excute_path = sys.path[0] + "/"
changedFiles_save_path = excute_path + "docs/changedFiles.txt"

subprocess.check_output("git add .", shell=True)
diffFilesStr = subprocess.check_output("git status", shell=True).decode()
diffFiles = diffFilesStr.split("\n")

with open(changedFiles_save_path, 'w', encoding='utf-8') as f:
    for diffFile in diffFiles :
        for map_file in update_map_files :
            if (map_file in diffFile) :
                f.write(diffFile + '\n')
#                print(diffFile)
    f.close()
#    json.dump(changedFiles, f, indent=2)


