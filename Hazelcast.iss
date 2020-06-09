[Files]
Source: {#DistDir}\*; DestDir: {app}; Flags: recursesubdirs

[Setup]
AppName={#MyAppName}
AppId={#MyAppId}
AppVerName={#MyAppName} {#MyAppVersion}
DefaultDirName={autopf}\{#MyAppName}
DefaultGroupName={#MyAppName}
;LicenseFile=license\apache-v2-license.txt
LicenseFile=LICENSE
OutputBaseFilename={#MyAppName}_setup_{#MyAppVersion}_wjre
VersionInfoVersion={#MyAppVersionWin}
VersionInfoCompany=Hazelcast
VersionInfoDescription=Hazelcast In-Memory Data Grid (IMDG)
AppPublisher=Hazelcast
AppSupportURL=https://hazelcast.org/
AppVersion={#MyAppVersion}
OutputDir={#OutputDir}
WizardStyle=modern
Compression=lzma2
SolidCompression=yes
ArchitecturesAllowed=x64
ArchitecturesInstallIn64BitMode=x64

[Icons]
Name: "{group}\Hazelcast {#MyAppVersion}"; Filename: "{app}\bin\Hazelcast.exe"
Name: "{group}\Hazelcast Client Demo"; Filename: "{app}\bin\HazelcastClient.exe"
Name: {group}\Uninstall {#MyAppName}; Filename: {uninstallexe}; IconFilename: "{app}\hazelcast.ico"
