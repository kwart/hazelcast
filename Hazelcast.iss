[Files]
Source: {#DistDir}\*; DestDir: {app}; Flags: recursesubdirs; AfterInstall: AddLogPropertiesPath()

[Setup]
AppName={#MyAppName}
AppId={#MyAppId}
AppVerName={#MyAppName} {#MyAppVersion}
DefaultDirName={autopf}\{#MyAppName}
DefaultGroupName={#MyAppName}
LicenseFile=LICENSE
OutputBaseFilename={#MyAppName}_setup_{#MyAppVersion}_wjre
VersionInfoVersion={#MyAppVersionWin}
VersionInfoCompany=Hazelcast
VersionInfoDescription=Hazelcast In-Memory Data Grid (IMDG)
AppPublisher=Hazelcast
AppSupportURL=https://hazelcast.org/
AppVersion={#MyAppVersion}
OutputDir={#OutputDir}
;WizardStyle=modern
Compression=lzma2
SolidCompression=yes
ArchitecturesAllowed=x64
ArchitecturesInstallIn64BitMode=x64
UninstallDisplayIcon={app}\bin\Hazelcast.exe

[Icons]
Name: "{group}\Hazelcast {#MyAppVersion}"; Filename: "{app}\bin\Hazelcast.exe"
Name: "{group}\Hazelcast Client Demo"; Filename: "{app}\bin\HazelcastClient.exe"
Name: "{group}\Uninstall {#MyAppName}"; Filename: "{uninstallexe}"

[Run]
Filename: "{app}\bin\prunsrv.exe"; Parameters: "install Hazelcast --DisplayName=""Hazelcast In-Memory Data Grid"" --Jvm=""{app}\jre\bin\server\jvm.dll"" --Startup=manual --Classpath=""{app}\lib\*"" --StartMode=jvm --StartClass=com.hazelcast.core.server.HazelcastServiceStarter --StartMethod=start --StopMode=jvm --StopClass=com.hazelcast.core.server.HazelcastServiceStarter --StopMethod=stop --LogPath=""{app}\logs"" --LogPrefix=service --StdOutput=auto --StdError=auto"

[UninstallRun]
Filename: "{app}\bin\prunsrv.exe"; Parameters: "stop Hazelcast"
Filename: "{app}\bin\prunsrv.exe"; Parameters: "delete Hazelcast"

[UninstallDelete]
Type: filesandordirs; Name: "{app}\logs"

[Code]
const L4JINI='.l4j.ini';

procedure AddLogPropertiesPath();
var
  fileExt: String;
  pathLen: integer;
  len: integer;
begin
  len := length(L4JINI);
  pathLen := length(CurrentFileName);
  if pathLen>len then begin
    fileExt := copy(CurrentFileName,  pathLen-len+1, len);
    if fileExt = L4JINI then begin
      SaveStringToFile(ExpandConstant(CurrentFileName), '' #13#10 '"-Djava.util.logging.config.file=' 
        + ExpandConstant('{app}') + '\bin\logging.properties"' #13#10, True);
    end;
  end;
end;