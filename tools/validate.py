import json, pathlib, xml.etree.ElementTree as ET
root=pathlib.Path(__file__).resolve().parents[1]/'app/src/main'
files=list(root.rglob('*.xml'));motions=list((root/'res/raw').glob('*.json'))
for f in files: ET.parse(f)
for f in motions:
 d=json.loads(f.read_text());assert 'layers' in d
print(f'Validated {len(files)} XML and {len(motions)} Lottie JSON files')
