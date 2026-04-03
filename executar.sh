#!/bin/bash
# ------------------------------------------------------------------------------
# Script de Automacao para Linux e macOS - Simulador de Escalonamento 2026
# ------------------------------------------------------------------------------

# Navegar para o diretorio onde o script esta
cd "$(dirname "$0")"

echo "===================================================="
echo "    SIMULADOR DE ESCALONAMENTO - MESTRADO 2026      "
echo "===================================================="
echo ""

# Verificar versoes do Java
echo "Verificando versoes instaladas:"
java -version
javac -version
echo ""

# Criar pastas se nao existirem
mkdir -p bin
mkdir -p exports

# Compilar o codigo
echo "Compilando codigos-fonte em src/..."
javac -d bin src/Processo.java src/SimuladorApp.java src/SimuladorGUI.java

if [ $? -eq 0 ]; then
    echo "Sucesso: Compilacao concluida na pasta bin/."
    echo "Iniciando Simulador Grafico..."
    # Rodar em background (&) para liberar o terminal
    java -cp bin SimuladorGUI &
    echo "Programa iniciado. Voce pode fechar este terminal."
else
    echo "ERRO: Falha na compilacao. Verifique as mensagens de erro acima."
    read -p "Pressione [Enter] para sair..."
fi
