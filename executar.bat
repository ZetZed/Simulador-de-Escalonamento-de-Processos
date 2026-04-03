@echo off
cd /d "%~dp0"
echo Verificando versoes:
java -version
javac -version
echo.

if not exist bin mkdir bin
if not exist exports mkdir exports

echo Compilando Simulador (GUI)...
javac -d bin src/Processo.java src/SimuladorApp.java src/SimuladorGUI.java
if %errorlevel% neq 0 (
    echo Erro na compilacao. Verifique as mensagens acima em src/.
    pause
    exit /b %errorlevel%
)
echo Compilacao concluida em bin/.
echo Rodando Simulador Grafico Pro...
start javaw -cp bin SimuladorGUI
exit
