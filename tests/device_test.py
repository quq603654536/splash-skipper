#!/usr/bin/env python3
"""Tests only the local fixture; never taps third-party account or permission pages."""
from pathlib import Path
import json, subprocess, sys, time

ROOT=Path(__file__).resolve().parent.parent
if len(sys.argv)<2:raise SystemExit('Usage: python3 tests/device_test.py DEVICE_SERIAL')
SERIAL=sys.argv[1]
def adb(*args):
    return subprocess.run(['adb','-s',SERIAL,*args],check=True,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT).stdout

results=[]
for scenario,expected,delay in [('countdown',True,1.8),('plainAd',True,1.8),('onboarding',False,13.0),('privacy',False,13.0),('gesture',True,2.0)]:
    adb('shell','am','start','-W','-n','com.wkm.splashskip/.MainActivity');time.sleep(.4)
    adb('shell','am','start','-S','-W','-n','com.wkm.splashskip.fixture/.MainActivity','--es','case',scenario)
    pid=adb('shell','pidof','com.wkm.splashskip.fixture').strip()
    time.sleep(delay)
    logs=adb('logcat','-d','--pid='+pid,'-v','brief','-s','SplashSkipFixture:I','*:S')
    actual='CLICKED '+scenario in logs
    passed=actual==expected
    result={'case':scenario,'expected_click':expected,'observed_click':actual,'pass':passed,'observation_seconds':delay}
    results.append(result);print(json.dumps(result),flush=True)
    if not passed:print(logs,flush=True)
(ROOT/'build/device-test-results.json').write_text(json.dumps(results,indent=2))
if not all(r['pass'] for r in results):raise SystemExit(1)
print('PASS: all fixture scenarios',flush=True)
