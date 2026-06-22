Set WshShell = CreateObject("WScript.Shell")
script = Left(WScript.ScriptFullName, InStrRev(WScript.ScriptFullName, "\")) & "tray_agent.py"
WshShell.Run "pythonw """ & script & """", 0, False
