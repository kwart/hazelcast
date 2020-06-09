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
Filename: "{app}\bin\prunsrv.exe"; Parameters: "install Hazelcast --DisplayName=""Hazelcast In-Memory Data Grid"" --Jvm=""{app}\jre\bin\server\jvm.dll"" --Startup=manual --Classpath=""{app}\lib\*"" --StartMode=jvm --StartClass=com.hazelcast.core.server.HazelcastServiceStarter --StartMethod=start --StopMode=jvm --StopClass=com.hazelcast.core.server.HazelcastServiceStarter --StopMethod=stop --LogPath=""{app}\logs"" --LogPrefix=service --StdOutput=../logs/hazelcast-out.log --StdError=../logs/hazelcast-err.log --JvmOptions9=""-Djava.util.logging.config.file={app}\bin\logging.properties"""

;Filename: "{app}\bin\prunsrv.exe"; Parameters: "install Hazelcast --DisplayName=""Hazelcast In-Memory Data Grid"" --Jvm=""{app}\jre\bin\server\jvm.dll"" --Startup=manual --Classpath=""{app}\lib\*"" --StartMode=jvm --StartClass=com.hazelcast.core.server.HazelcastServiceStarter --StartMethod=start --StopMode=jvm --StopClass=com.hazelcast.core.server.HazelcastServiceStarter --StopMethod=stop --LogPath=""{app}\logs"" --LogPrefix=service --StdOutput=auto --StdError=auto --JvmOptions9=""-Djava.util.logging.config.file={app}\bin\logging.properties"""
; ++JvmOptions9=""--add-modules java.se"" ++JvmOptions9=""--add-exports java.base/jdk.internal.ref=ALL-UNNAMED"" ++JvmOptions9=""--add-opens java.base/java.lang=ALL-UNNAMED"" ++JvmOptions9=""--add-opens java.base/java.nio=ALL-UNNAMED"" ++JvmOptions9=""--add-opens java.base/sun.nio.ch=ALL-UNNAMED"" ++JvmOptions9=""--add-opens java.management/sun.management=ALL-UNNAMED"" ++JvmOptions9=""--add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED""
; ++JvmOptions9=--add-modules ++JvmOptions9=java.se ++JvmOptions9=--add-exports ++JvmOptions9=java.base/jdk.internal.ref=ALL-UNNAMED ++JvmOptions9=--add-opens ++JvmOptions9=java.base/java.lang=ALL-UNNAMED ++JvmOptions9=--add-opens ++JvmOptions9=java.base/java.nio=ALL-UNNAMED ++JvmOptions9=--add-opens ++JvmOptions9=java.base/sun.nio.ch=ALL-UNNAMED ++JvmOptions9=--add-opens ++JvmOptions9=java.management/sun.management=ALL-UNNAMED ++JvmOptions9=--add-opens ++JvmOptions9=jdk.management/com.sun.management.internal=ALL-UNNAMED

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
