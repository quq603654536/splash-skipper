#!/usr/bin/env python3
"""Dependency-free Java/Android build using official SDK tools."""
from pathlib import Path
import hashlib, os, secrets, shutil, subprocess, sys, zipfile

ROOT=Path(__file__).resolve().parent
JAVA=Path(os.environ.get('JAVA_HOME','/Applications/GoLand.app/Contents/jbr/Contents/Home'))
ANDROID=next((ROOT/'tools/platform').rglob('android.jar'))
BUILD_TOOLS=next((ROOT/'tools/build-tools').rglob('aapt2')).parent
ENV=dict(os.environ,JAVA_HOME=str(JAVA),PATH=str(JAVA/'bin')+os.pathsep+os.environ.get('PATH',''))

def run(args): subprocess.run([str(x) for x in args],check=True,cwd=ROOT,env=ENV)

def signing():
    folder=ROOT/'.signing';folder.mkdir(exist_ok=True,mode=0o700)
    password=folder/'password.txt';key=folder/'splash-skipper.p12'
    if not password.exists():
        fd=os.open(password,os.O_WRONLY|os.O_CREAT|os.O_EXCL,0o600)
        with os.fdopen(fd,'w') as f:f.write(secrets.token_urlsafe(32)+'\n')
    if not key.exists():
        run([JAVA/'bin/keytool','-genkeypair','-keystore',key,'-storetype','PKCS12','-alias','splashskip',
             '-keyalg','RSA','-keysize','2048','-sigalg','SHA256withRSA','-validity','3650',
             '-dname','CN=Splash Skipper Local','-storepass:file',password,'-keypass:file',password])
        key.chmod(0o600)
    return key,password

def build(module,name):
    src=ROOT/module/'src/main';out=ROOT/'build'/module
    if out.exists():shutil.rmtree(out)
    for sub in ['classes','gen','dex']: (out/sub).mkdir(parents=True,exist_ok=True)
    resources=out/'resources.zip'
    resource_dir=src/'res'
    link=[BUILD_TOOLS/'aapt2','link','-I',ANDROID,'--manifest',src/'AndroidManifest.xml','--java',out/'gen',
          '--min-sdk-version','26','--target-sdk-version','36','-o',out/'resources.apk']
    if resource_dir.exists():
        run([BUILD_TOOLS/'aapt2','compile','--dir',resource_dir,'-o',resources]);link.extend(['-R',resources,'--auto-add-overlay'])
    run(link)
    sources=list((src/'java').rglob('*.java'))+list((out/'gen').rglob('*.java'))
    run([JAVA/'bin/javac','--release','8','-encoding','UTF-8','-classpath',ANDROID,'-d',out/'classes',*sources])
    run([JAVA/'bin/java','-cp',BUILD_TOOLS/'lib/d8.jar','com.android.tools.r8.D8','--lib',ANDROID,
         '--min-api','26','--output',out/'dex',*list((out/'classes').rglob('*.class'))])
    unsigned=out/'unsigned.apk';shutil.copy2(out/'resources.apk',unsigned)
    with zipfile.ZipFile(unsigned,'a',compression=zipfile.ZIP_DEFLATED) as z:
        for dex in (out/'dex').glob('*.dex'):z.write(dex,dex.name)
    run([BUILD_TOOLS/'zipalign','-f','4',unsigned,out/'aligned.apk'])
    key,password=signing();dist=ROOT/'dist';dist.mkdir(exist_ok=True);apk=dist/name
    run([BUILD_TOOLS/'apksigner','sign','--ks',key,'--ks-key-alias','splashskip','--ks-pass','file:'+str(password),
         '--v1-signer-name','CERT','--v4-signing-enabled','false','--out',apk,out/'aligned.apk'])
    run([BUILD_TOOLS/'apksigner','verify','--verbose',apk])
    print('APK:',apk);print('SHA256:',hashlib.sha256(apk.read_bytes()).hexdigest())

if __name__=='__main__':
    fixture='--fixture' in sys.argv
    build('fixture' if fixture else 'app','splash-skipper-fixture.apk' if fixture else 'splash-skipper-1.0.3.apk')
