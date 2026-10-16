; Extra steps run by the electron-builder NSIS installer.
!macro customInstall
  ; Let the phone reach the server and discovery on the local network (no firewall prompt for the user).
  nsExec::Exec 'netsh advfirewall firewall delete rule name="Stream Companion"'
  nsExec::Exec 'netsh advfirewall firewall add rule name="Stream Companion" dir=in action=allow program="$INSTDIR\${APP_EXECUTABLE_FILENAME}" enable=yes profile=any description="Stream Companion media server"'
!macroend

!macro customUnInstall
  nsExec::Exec 'netsh advfirewall firewall delete rule name="Stream Companion"'
!macroend
