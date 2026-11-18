; Extra NSIS steps for both installers (included by electron-builder).
; The admin server listens on port 8081 for tills on the shop network: allow it through Windows Firewall.
!macro customInstall
  ${If} ${FileExists} "$INSTDIR\resources\server\admin.jar"
    nsExec::Exec 'netsh advfirewall firewall add rule name="Retail POS Admin Server" dir=in action=allow protocol=TCP localport=8081'
  ${EndIf}
!macroend
!macro customUnInstall
  nsExec::Exec 'netsh advfirewall firewall delete rule name="Retail POS Admin Server"'
!macroend
