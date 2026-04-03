import java.util.*;

public class Processo {
    public enum Estado { PRONTO, EXECUTANDO, ESPERANDO_ES, CONCLUIDO }

    private final String id;
    private final int tempoChegada;
    private final int tempoExecucaoTotal;
    private final int prioridade;
    
    // Lista de quando ocorrem E/S (tempo_cpu_acumulado -> duracao_es)
    private final Map<Integer, Integer> eventosES = new HashMap<>();

    private int tempoCPUAcumulado = 0;
    private int tempoEsperaES = 0;
    private int tempoTotalEspera = 0;
    private int tempoRetorno = 0;
    private int tempoResposta = -1;
    private Estado estado = Estado.PRONTO;

    public Processo(String id, int tempoChegada, int tempoExecucao, int prioridade) {
        this.id = id;
        this.tempoChegada = tempoChegada;
        this.tempoExecucaoTotal = tempoExecucao;
        this.prioridade = prioridade;
    }

    public void adicionarES(int tempoCPU, int duracao) {
        eventosES.put(tempoCPU, duracao);
    }

    public Processo clonar() {
        Processo p = new Processo(id, tempoChegada, tempoExecucaoTotal, prioridade);
        p.eventosES.putAll(this.eventosES);
        return p;
    }

    // Getters
    public String getId() { return id; }
    public int getTempoChegada() { return tempoChegada; }
    public int getTempoExecucaoTotal() { return tempoExecucaoTotal; }
    public int getPrioridade() { return prioridade; }
    public int getTempoCPUAcumulado() { return tempoCPUAcumulado; }
    public int getTempoTotalEspera() { return tempoTotalEspera; }
    public int getTempoRetorno() { return tempoRetorno; }
    public int getTempoResposta() { return tempoResposta; }
    public Estado getEstado() { return estado; }
    public int getTempoEsperaES() { return tempoEsperaES; }

    // Setters e Métodos de Transição
    public void setEstado(Estado novoEstado, int tempoSimulacao) {
        if (this.estado == Estado.PRONTO && novoEstado == Estado.EXECUTANDO && tempoResposta == -1) {
            tempoResposta = tempoSimulacao - tempoChegada;
        }
        this.estado = novoEstado;
    }

    public void incrementarCPU() { tempoCPUAcumulado++; }
    public void incrementarEspera() { tempoTotalEspera++; }
    public void decrementarEsperaES() { tempoEsperaES--; }
    public void setTempoEsperaES(int d) { tempoEsperaES = d; }
    public void setTempoRetorno(int t) { tempoRetorno = t; }

    public Integer verificarEventoES() {
        return eventosES.get(tempoCPUAcumulado);
    }

    @Override
    public String toString() { return id; }
}
