#!/usr/bin/env python3
import os
import re

def main():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    project_root = os.path.dirname(os.path.dirname(script_dir))
    
    # 1. Parse app/build.gradle.kts
    gradle_path = os.path.join(project_root, 'app', 'build.gradle.kts')
    with open(gradle_path, 'r', encoding='utf-8') as f:
        gradle_content = f.read()
        
    version_name_match = re.search(r'versionName\s*=\s*"([^"]+)"', gradle_content)
    version_code_match = re.search(r'versionCode\s*=\s*(\d+)', gradle_content)
    
    if not version_name_match or not version_code_match:
        print("Error: Could not parse versionName or versionCode from build.gradle.kts")
        return
        
    version_name = version_name_match.group(1)
    version_code = version_code_match.group(1)
    
    # 2. Read changelog
    changelog_path = os.path.join(project_root, 'fastlane', 'metadata', 'android', 'en-US', 'changelogs', f'{version_code}.txt')
    if not os.path.exists(changelog_path):
        print(f"Error: Changelog file {changelog_path} not found")
        return
        
    with open(changelog_path, 'r', encoding='utf-8') as f:
        changelog = f.read().strip()
        
    # 3. Create formatted release notes content
    release_notes_content = f"""### 💖 Support Our Work
*   We are committed to making our apps as powerful and polished as possible. As an entirely community-funded project, we rely on your support to keep going, please consider becoming a [sponsor](https://github.com/sponsors/LeanBitLab). A huge thank you to all our current supporters!

## 🚀 What's New
{changelog}

## 📦 Downloads (Choose Your Flavor)

| File | Description | Permissions |
| :--- | :--- | :--- |
| **`1-LeanType_{version_name}-standardfull-release.apk`** | **Recommended**. Cloud AI | Internet | 
| **`2-LeanType_{version_name}-standard-release.apk`** | **Fdroid Build**. Standard + No Handwrite | Internet |
| **`3-LeanType_{version_name}-offline-release.apk`** | **Privacy Focused**. No Internet. Offline AI Only. | No Internet |
| **`4-LeanType_{version_name}-offlinelite-release.apk`** | **Minimalist**. Pure FOSS. No AI code. | No Internet |
"""

    # 4. Write to docs/releasenote/release_notes_v{version_name}.md
    out_dir = os.path.join(project_root, 'docs', 'releasenote')
    os.makedirs(out_dir, exist_ok=True)
    out_path = os.path.join(out_dir, f'release_notes_v{version_name}.md')
    with open(out_path, 'w', encoding='utf-8') as f:
        f.write(release_notes_content)
    temp_path = os.path.join(out_dir, 'release_notes_temp.md')
    with open(temp_path, 'w', encoding='utf-8') as f:
        f.write(release_notes_content)
        
    print(f"Successfully generated {out_path} and {temp_path}")

if __name__ == '__main__':
    # ponytail: minimal release notes generator using standard library
    main()
