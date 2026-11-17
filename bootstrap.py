#!/usr/bin/env python3
"""Download the pinned Android SDK pieces from Google's official repository."""
from pathlib import Path
import concurrent.futures, hashlib, os, urllib.request, zipfile

ROOT = Path(__file__).resolve().parent
TOOLS = ROOT / 'tools'
PACKAGES = [
    ('platform-36_r02.zip', '2c1a80dd4d9f7d0e6dd336ec603d9b5c55a6f576', 'platform'),
    ('build-tools_r36_macosx.zip', '199ae0047ee61e842f8ee0c6d3918e44fb9a1f83', 'build-tools'),
]

def download(item):
    name, checksum, destination = item
    archive = TOOLS / name
    if not archive.exists():
        urllib.request.urlretrieve('https://dl.google.com/android/repository/' + name, archive)
    assert hashlib.sha1(archive.read_bytes()).hexdigest() == checksum, name
    target = TOOLS / destination
    if not target.exists():
        with zipfile.ZipFile(archive) as z:
            for info in z.infolist():
                path = target / info.filename
                if not path.resolve().is_relative_to(target.resolve()):
                    raise ValueError('Unsafe archive path')
                z.extract(info, target)
                mode = info.external_attr >> 16
                if mode and path.is_file():
                    path.chmod(mode & 0o777)
    print('Ready:', destination, flush=True)

if __name__ == '__main__':
    TOOLS.mkdir(exist_ok=True)
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
        list(pool.map(download, PACKAGES))
