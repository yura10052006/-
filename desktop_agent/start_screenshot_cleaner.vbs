Set WshShell = CreateObject("WScript.Shell")
WshShell.Run "python """ & WScript.ScriptFullName & """\..\screenshot_cleaner.py""", 0, False
