import csv
import json
import os
import zipfile
from collections import defaultdict


def main() -> None:
    repo = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
    apk = os.path.join(
        repo, "app", "build", "intermediates", "apk", "debug", "Mupen64PlusAE-debug.apk"
    )
    out_root = os.path.join(repo, "design", "touchscreen-assets")
    extract_root = os.path.join(out_root, "skins")
    os.makedirs(extract_root, exist_ok=True)

    if not os.path.isfile(apk):
        raise SystemExit(f"APK not found: {apk}")

    prefix = "assets/mupen64plus_data/skins/touchscreen/"

    required = [
        "analog-back.png",
        "analog-fore.png",
        "analog.png",
        "dpad.png",
        "dpad-mask.png",
        "groupAB.png",
        "groupAB-mask.png",
        "groupAB-holdA.png",
        "groupAB-holdB.png",
        "groupC.png",
        "groupC-mask.png",
        "groupC-holdCu.png",
        "groupC-holdCd.png",
        "groupC-holdCl.png",
        "groupC-holdCr.png",
        "buttonA.png",
        "buttonA-mask.png",
        "buttonA-holdA.png",
        "buttonB.png",
        "buttonB-mask.png",
        "buttonB-holdB.png",
        "buttonCu.png",
        "buttonCu-mask.png",
        "buttonCu-holdCu.png",
        "buttonCr.png",
        "buttonCr-mask.png",
        "buttonCr-holdCr.png",
        "buttonCd.png",
        "buttonCd-mask.png",
        "buttonCd-holdCd.png",
        "buttonCl.png",
        "buttonCl-mask.png",
        "buttonCl-holdCl.png",
        "buttonL.png",
        "buttonL-mask.png",
        "buttonL-holdL.png",
        "buttonR.png",
        "buttonR-mask.png",
        "buttonR-holdR.png",
        "buttonZ.png",
        "buttonZ-mask.png",
        "buttonZ-holdZ.png",
        "buttonS.png",
        "buttonS-mask.png",
        "buttonS-holdS.png",
        "buttonSen.png",
        "buttonSen-mask.png",
        "buttonSen-holdSen.png",
        "fps.png",
        "fps-0.png",
        "fps-1.png",
        "fps-2.png",
        "fps-3.png",
        "fps-4.png",
        "fps-5.png",
        "fps-6.png",
        "fps-7.png",
        "fps-8.png",
        "fps-9.png",
        "skin.ini",
    ]

    skin_files = defaultdict(list)
    rows = []

    with zipfile.ZipFile(apk) as zf:
        for name in zf.namelist():
            if not name.startswith(prefix):
                continue
            rel = name[len(prefix) :]
            if not rel or rel.endswith("/"):
                continue

            parts = rel.split("/", 1)
            if len(parts) != 2:
                continue

            skin, filename = parts
            skin_files[skin].append(filename)

        for skin, files in skin_files.items():
            for filename in files:
                src = f"{prefix}{skin}/{filename}"
                dst = os.path.join(extract_root, skin, filename)
                os.makedirs(os.path.dirname(dst), exist_ok=True)
                with zf.open(src) as rf, open(dst, "wb") as wf:
                    wf.write(rf.read())

    skins = sorted(skin_files.keys())

    csv_path = os.path.join(out_root, "touchscreen_assets_manifest.csv")
    with open(csv_path, "w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)
        writer.writerow(
            ["skin", "file_count", "png_count", "missing_required_count", "missing_required_files"]
        )
        for skin in skins:
            files = sorted(set(skin_files[skin]))
            png_count = sum(1 for x in files if x.lower().endswith(".png"))
            missing = [x for x in required if x not in files]
            writer.writerow([skin, len(files), png_count, len(missing), ";".join(missing)])
            rows.append((skin, len(files), png_count, missing))

    md_path = os.path.join(out_root, "touchscreen_assets_manifest.md")
    with open(md_path, "w", encoding="utf-8") as f:
        f.write("# Touchscreen Assets Manifest\n\n")
        f.write("Nguon: `app/build/intermediates/apk/debug/Mupen64PlusAE-debug.apk`\n\n")
        f.write(f"- Tong skin: **{len(skins)}**\n")
        f.write("- Bo file da extract: `design/touchscreen-assets/skins/`\n\n")
        f.write("| Skin | Files | PNG | Missing required |\n")
        f.write("|---|---:|---:|---:|\n")
        for skin, total, png_count, missing in rows:
            f.write(f"| {skin} | {total} | {png_count} | {len(missing)} |\n")

    summary = {
        "skins": len(skins),
        "total_files_extracted": sum(len(set(skin_files[s])) for s in skins),
        "output_root": out_root,
        "skins_list": skins,
    }
    with open(os.path.join(out_root, "summary.json"), "w", encoding="utf-8") as f:
        json.dump(summary, f, indent=2)

    print("DONE")
    print("output:", out_root)
    print("skins:", len(skins))
    print("total_files_extracted:", summary["total_files_extracted"])


if __name__ == "__main__":
    main()
