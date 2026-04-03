# Simulador de Escalonamento de Processos - 2026

Este projeto e um simulador de escalonamento de processos desenvolvido para a disciplina de **Sistemas Operacionais**. Ele fornece uma interface grafica (GUI) moderna para visualizar como o processador gerencia diferentes tarefas.

## 👤 Desenvolvedor

**Diego Rodrigues de Souza Arimura**
Mestrando em Computação Aplicada/ Sistemas Operacionais

---

## 🚀 Como Executar

Nao é necessario instalar nada além do Java. O projeto ja vem com um script de automação.

1. Garanta que o **JDK 17** (ou superior) esteja no PATH do seu sistema.
2. De um duplo clique no arquivo: `executar.bat`

O script ira compilar as fontes e abrir a interface gráfica. O terminal será fechado automaticamente após o lançamento.

---

## 🛠️ Funcionalidades

- **Algoritmos Implementados**: FCFS, SJF (Não Preemptivo), SRTF (Preemptivo), Prioridade (Não Preemptivo e Preemptivo) e Round Robin.
- **Gráfico de Gantt Animado**: Visualização em tempo real da execução com auto-scroll horizontal.
- **Dashboard de Estatísticas**: Cálculo automático de Tempo Médio de Espera e Retorno.
- **Exportação de Relatório**: Salva o gráfico atual em formato PNG com cabeçalho técnico na pasta `/exports`.
- **Tratamento de E/S**: Simulação real de bloqueios de Entrada e Saída.

---

## 📋 Como Usar

1. Clique em **"Buscar Arquivo..."** e selecione um arquivo de texto (ex: `entrada.txt`).
2. Escolha o **Algoritmo** de escalonamento no menu suspenso.
3. Se utilizar **Round Robin**, defina o valor do **Quantum**.
4. Clique em **"Simular"** para iniciar a animação.
5. Utilize a barra de rolagem horizontal se a timeline for muito longa.
6. Ao final, você pode exportar o resultado clicando em **"Exportar Gantt"**.

---

## 📂 Estrutura do Projeto

- `src/`: Códigos-fonte Java.
- `bin/`: Arquivos compilados (.class).
- `exports/`: Pasta onde são salvas as imagens exportadas.
- `README.md`: Este guia de uso.
- `t1.pdf`: Enunciado dos requisitos do trabalho.

---

*Projeto desenvolvido para fins acadêmicos - 2026*
