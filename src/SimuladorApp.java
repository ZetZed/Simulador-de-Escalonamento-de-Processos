import java.io.File;
import java.util.*;

public class SimuladorApp {

    public static void main(String[] args) {
        try (Scanner sc = new Scanner(System.in)) {
            System.out.println("======================================================");
            System.out.println("      Simulador de Algoritmos de Escalonamento      ");
            System.out.println("            SO 2026 - Mestrado UFMS                ");
            System.out.println("======================================================");
            System.out.println("Formato do arquivo: ID Chegada CPU Prioridade [IO_Disparo IO_DURACAO]");
            System.out.print("Caminho do arquivo (default: entrada.txt): ");
            String caminho = sc.nextLine().trim();
            if (caminho.isEmpty()) caminho = "entrada.txt";

            LeituraResultado lr = lerArquivo(caminho);
            if (lr.processos() == null || lr.processos().isEmpty()) {
                System.out.println("Nenhum processo carregado ou arquivo inválido.");
                return;
            }
            List<Processo> processos = lr.processos();

            while (true) {
                System.out.println("\n--- Algoritmos ---");
                System.out.println("1. FCFS");
                System.out.println("2. SJF (Não Preemptivo)");
                System.out.println("3. SRTF (SJF Preemptivo/Mais Curto Primeiro)");
                System.out.println("4. Prioridade (Não Preemptivo)");
                System.out.println("5. Prioridade (Preemptivo)");
                System.out.println("6. Round Robin");
                System.out.println("7. Sair");
                System.out.print("Escolha: ");
                
                String opt = sc.nextLine();
                if (opt.equals("7")) break;

                List<Processo> copia = processos.stream().map(Processo::clonar).toList();
                
                switch (opt) {
                    case "1" -> simular(copia, "FCFS", false, null);
                    case "2" -> simular(copia, "SJF", false, Comparator.comparingInt(Processo::getTempoExecucaoTotal));
                    case "3" -> simular(copia, "SRTF", true, Comparator.comparingInt(p -> (p.getTempoExecucaoTotal() - p.getTempoCPUAcumulado())));
                    case "4" -> simular(copia, "Prioridade", false, Comparator.comparingInt(Processo::getPrioridade));
                    case "5" -> simular(copia, "Prioridade Preemptiva", true, Comparator.comparingInt(Processo::getPrioridade));
                    case "6" -> {
                        System.out.print("Quantum: ");
                        int q = Integer.parseInt(sc.nextLine());
                        simularRR(copia, q);
                    }
                    default -> System.out.println("Opção inválida.");
                }
            }
        }
    }

    public record LogEvento(int tempo, String idProcesso, String acao) {}
    public record ResultadoSimulacao(List<Processo> concluidos, List<LogEvento> timeline) {}
    public record LeituraResultado(List<Processo> processos, List<String> erros) {}

    public static LeituraResultado lerArquivo(String caminho) {
        List<Processo> lista = new ArrayList<>();
        List<String> erros = new ArrayList<>();
        File file = new File(caminho);
        
        if (!file.exists()) return new LeituraResultado(null, List.of("Arquivo não encontrado."));
        
        // Segurança: Limite de 1MB para arquivo de texto
        if (file.length() > 1024 * 1024) {
            return new LeituraResultado(null, List.of("Arquivo muito grande (Limite: 1MB)."));
        }

        try (Scanner fs = new Scanner(file)) {
            int linhaNum = 0;
            while (fs.hasNextLine()) {
                linhaNum++;
                String line = fs.nextLine().trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                
                try {
                    String[] parts = line.split("\\s+");
                    if (parts.length < 4) {
                        erros.add("Linha " + linhaNum + ": Formato incompleto.");
                        continue;
                    }
                    
                    String id = parts[0];
                    int chegada = Integer.parseInt(parts[1]);
                    int cpu = Integer.parseInt(parts[2]);
                    int prio = Integer.parseInt(parts[3]);
                    
                    if (chegada < 0 || cpu <= 0) {
                        erros.add("Linha " + linhaNum + ": Valores de tempo inválidos.");
                        continue;
                    }

                    Processo p = new Processo(id, chegada, cpu, prio);
                    if (parts.length >= 6) {
                        p.adicionarES(Integer.parseInt(parts[4]), Integer.parseInt(parts[5]));
                    }
                    lista.add(p);
                } catch (NumberFormatException e) {
                    erros.add("Linha " + linhaNum + ": Contém caracteres não numéricos.");
                }
            }
        } catch (Exception e) {
            return new LeituraResultado(null, List.of("Erro crítico na leitura: " + e.getMessage()));
        }
        return new LeituraResultado(lista, erros);
    }

    public static ResultadoSimulacao simular(List<Processo> processos, String nome, boolean preemptivo, Comparator<Processo> comp) {
        int tempo = 0;
        List<Processo> prontos = new ArrayList<>();
        List<Processo> esperandoES = new ArrayList<>();
        List<Processo> concluidos = new ArrayList<>();
        List<LogEvento> timeline = new ArrayList<>();
        Processo emExecucao = null;

        while (concluidos.size() < processos.size() && tempo < 500) { // Safety break
            // 1. Chegada
            for (Processo p : processos) {
                if (p.getTempoChegada() == tempo) {
                    prontos.add(p);
                }
            }

            // 2. E/S
            Iterator<Processo> itES = esperandoES.iterator();
            while (itES.hasNext()) {
                Processo p = itES.next();
                p.decrementarEsperaES();
                if (p.getTempoEsperaES() <= 0) {
                    p.setEstado(Processo.Estado.PRONTO, tempo);
                    prontos.add(p);
                    itES.remove();
                }
            }

            // 3. Preempção
            if (preemptivo && emExecucao != null && !prontos.isEmpty()) {
                Processo melhor = prontos.stream().min(comp).orElse(null);
                if (melhor != null && comp.compare(melhor, emExecucao) < 0) {
                    emExecucao.setEstado(Processo.Estado.PRONTO, tempo);
                    prontos.add(emExecucao);
                    emExecucao = null;
                }
            }

            // 4. Selecionar
            if (emExecucao == null && !prontos.isEmpty()) {
                if (comp != null) prontos.sort(comp);
                emExecucao = prontos.remove(0);
                emExecucao.setEstado(Processo.Estado.EXECUTANDO, tempo);
            }

            // 5. Timeline e Executar
            if (emExecucao != null) {
                timeline.add(new LogEvento(tempo, emExecucao.getId(), "CPU"));
                emExecucao.incrementarCPU();
                Integer duracaoES = emExecucao.verificarEventoES();
                
                if (emExecucao.getTempoCPUAcumulado() >= emExecucao.getTempoExecucaoTotal()) {
                    emExecucao.setEstado(Processo.Estado.CONCLUIDO, tempo + 1);
                    emExecucao.setTempoRetorno(tempo + 1 - emExecucao.getTempoChegada());
                    concluidos.add(emExecucao);
                    emExecucao = null;
                } else if (duracaoES != null) {
                    emExecucao.setEstado(Processo.Estado.ESPERANDO_ES, tempo + 1);
                    emExecucao.setTempoEsperaES(duracaoES);
                    esperandoES.add(emExecucao);
                    emExecucao = null;
                }
            } else {
                timeline.add(new LogEvento(tempo, "ocioso", "IDLE"));
            }

            for (Processo p : prontos) p.incrementarEspera();
            tempo++;
        }
        return new ResultadoSimulacao(concluidos, timeline);
    }

    public static ResultadoSimulacao simularRR(List<Processo> processos, int quantum) {
        int tempo = 0;
        Queue<Processo> prontos = new LinkedList<>();
        List<Processo> esperandoES = new ArrayList<>();
        List<Processo> concluidos = new ArrayList<>();
        List<LogEvento> timeline = new ArrayList<>();
        Processo emExecucao = null;
        int qRestante = 0;

        while (concluidos.size() < processos.size() && tempo < 500) {
            for (Processo p : processos) {
                if (p.getTempoChegada() == tempo) {
                    prontos.add(p);
                }
            }

            Iterator<Processo> itES = esperandoES.iterator();
            while (itES.hasNext()) {
                Processo p = itES.next();
                p.decrementarEsperaES();
                if (p.getTempoEsperaES() <= 0) {
                    p.setEstado(Processo.Estado.PRONTO, tempo);
                    prontos.add(p);
                    itES.remove();
                }
            }

            if (emExecucao != null && qRestante <= 0) {
                emExecucao.setEstado(Processo.Estado.PRONTO, tempo);
                prontos.add(emExecucao);
                emExecucao = null;
            }

            if (emExecucao == null && !prontos.isEmpty()) {
                emExecucao = prontos.poll();
                emExecucao.setEstado(Processo.Estado.EXECUTANDO, tempo);
                qRestante = quantum;
            }

            if (emExecucao != null) {
                timeline.add(new LogEvento(tempo, emExecucao.getId(), "CPU"));
                emExecucao.incrementarCPU();
                qRestante--;
                
                Integer duracaoES = emExecucao.verificarEventoES();
                if (emExecucao.getTempoCPUAcumulado() >= emExecucao.getTempoExecucaoTotal()) {
                    emExecucao.setTempoRetorno(tempo + 1 - emExecucao.getTempoChegada());
                    concluidos.add(emExecucao);
                    emExecucao = null;
                } else if (duracaoES != null) {
                    emExecucao.setTempoEsperaES(duracaoES);
                    esperandoES.add(emExecucao);
                    emExecucao = null;
                }
            } else {
                timeline.add(new LogEvento(tempo, "ocioso", "IDLE"));
            }

            for (Processo p : prontos) p.incrementarEspera();
            tempo++;
        }
        return new ResultadoSimulacao(concluidos, timeline);
    }

}
