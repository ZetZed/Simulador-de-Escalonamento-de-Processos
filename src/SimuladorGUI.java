import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.Timer;

public class SimuladorGUI extends JFrame {
    // Cores Premium (Dark Mode)
    private static final Color BG_DARK = new Color(33, 37, 43);
    private static final Color PANEL_DARK = new Color(40, 44, 52);
    private static final Color ACCENT_BLUE = new Color(97, 175, 239);
    private static final Color TEXT_WHITE = new Color(171, 178, 191);
    private static final Color CARD_BG = new Color(44, 50, 60);

    private JLabel lblArquivoNome = new JLabel("Nenhum arquivo...");
    private String caminhoArquivoCompleto = "";
    private JButton btnBuscar = createStyledButton("Buscar Arquivo...", ACCENT_BLUE);
    private JButton btnSimular = createStyledButton("Simular", new Color(152, 195, 121));
    private JButton btnExportar = createStyledButton("Exportar Gantt", new Color(198, 120, 221));

    private JComboBox<String> comboAlgo = new JComboBox<>(new String[] {
            "FCFS", "SJF (Não Preemptivo)", "SRTF (Preemptivo)",
            "Prioridade (Não Preemptivo)", "Prioridade (Preemptivo)", "Round Robin"
    });
    private JLabel lblQuantum = new JLabel("Quantum:");
    private JSpinner spinQuantum = new JSpinner(new SpinnerNumberModel(2, 1, 20, 1));

    private JLabel lblMediaEspera = new JLabel("0.0 ms");
    private JLabel lblMediaRetorno = new JLabel("0.0 ms");
    private JScrollPane scrollGantt;

    // Variáveis para sincronizar a exportação com o que foi realmente simulado
    private String algoSimulado = "";
    private int quantumSimulado = 0;

    private DefaultTableModel tableModel;
    private GanttPanel ganttPanel = new GanttPanel();
    private List<SimuladorApp.LogEvento> fullTimeline = new ArrayList<>();
    private List<SimuladorApp.LogEvento> currentTimeline = new ArrayList<>();
    private Timer timer;

    public SimuladorGUI() {
        setTitle("Simulador de Escalonamento de Processos - 2026");
        setSize(1100, 800);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout(15, 15));

        // 1. Painel Superior: Configurações e Dashboard
        JPanel pnlHeader = new JPanel(new BorderLayout(10, 10));
        pnlHeader.setOpaque(false);
        pnlHeader.setBorder(new EmptyBorder(15, 20, 5, 20));

        // Configurações
        JPanel pnlConfig = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        pnlConfig.setBackground(PANEL_DARK);
        pnlConfig.setBorder(BorderFactory.createLineBorder(new Color(60, 65, 75), 1));

        JLabel lblArq = new JLabel("Arquivo:");
        lblArq.setForeground(TEXT_WHITE);
        lblArquivoNome.setForeground(new Color(209, 154, 102));

        pnlConfig.add(lblArq);
        pnlConfig.add(lblArquivoNome);
        pnlConfig.add(btnBuscar);

        JLabel lblAlg = new JLabel("Algoritmo:");
        lblAlg.setForeground(TEXT_WHITE);
        pnlConfig.add(lblAlg);
        pnlConfig.add(comboAlgo);

        lblQuantum.setForeground(TEXT_WHITE);
        pnlConfig.add(lblQuantum);
        pnlConfig.add(spinQuantum);
        lblQuantum.setVisible(false);
        spinQuantum.setVisible(false);

        pnlConfig.add(btnSimular);
        pnlConfig.add(btnExportar);

        pnlHeader.add(pnlConfig, BorderLayout.NORTH);

        // Dashboard (Cards)
        JPanel pnlStats = new JPanel(new GridLayout(1, 2, 20, 0));
        pnlStats.setOpaque(false);
        pnlStats.setBorder(new EmptyBorder(10, 0, 0, 0));
        pnlStats.add(createStatCard("Tempo Médio de Espera", lblMediaEspera));
        pnlStats.add(createStatCard("Tempo Médio de Retorno", lblMediaRetorno));
        pnlHeader.add(pnlStats, BorderLayout.CENTER);

        add(pnlHeader, BorderLayout.NORTH);

        // 2. Painel Central: Tabela
        JPanel pnlCenter = new JPanel(new BorderLayout());
        pnlCenter.setOpaque(false);
        pnlCenter.setBorder(new EmptyBorder(5, 20, 5, 20));

        String[] colunas = { "ID", "Chegada", "CPU Total", "Prioridade", "Espera", "Retorno", "Resposta" };
        tableModel = new DefaultTableModel(colunas, 0);
        JTable table = new JTable(tableModel);
        styleTable(table);

        JScrollPane scrollTable = new JScrollPane(table);
        scrollTable.getViewport().setBackground(PANEL_DARK);
        scrollTable.setBorder(BorderFactory.createLineBorder(new Color(60, 65, 75)));
        pnlCenter.add(scrollTable);
        add(pnlCenter, BorderLayout.CENTER);

        // 3. Painel Inferior: Gantt
        JPanel pnlBottom = new JPanel(new BorderLayout());
        pnlBottom.setOpaque(false);
        pnlBottom.setPreferredSize(new Dimension(0, 320));
        pnlBottom.setBorder(new EmptyBorder(5, 20, 20, 20));

        JPanel ganttContainer = new JPanel(new BorderLayout());
        ganttContainer.setBackground(PANEL_DARK);
        ganttContainer.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(60, 65, 75)),
                "Timeline de Execução (CPU)", 0, 0, null, TEXT_WHITE));

        scrollGantt = new JScrollPane(ganttPanel);
        scrollGantt.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollGantt.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scrollGantt.setBorder(null);
        scrollGantt.setBackground(PANEL_DARK);
        scrollGantt.getViewport().setBackground(PANEL_DARK);
        ganttContainer.add(scrollGantt);

        pnlBottom.add(ganttContainer);

        // Rodapé de Créditos (Discreto)
        JPanel pnlFooter = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pnlFooter.setOpaque(false);
        JLabel lblCredits = new JLabel("Desenvolvido por: Diego Rodrigues de Souza Arimura | Versão: 1.0 (PRO) ");
        lblCredits.setFont(new Font("SansSerif", Font.ITALIC, 11));
        lblCredits.setForeground(new Color(110, 115, 125));
        pnlFooter.add(lblCredits);
        pnlBottom.add(pnlFooter, BorderLayout.SOUTH);

        add(pnlBottom, BorderLayout.SOUTH);

        // Listeners
        comboAlgo.addActionListener(e -> {
            boolean isRR = "Round Robin".equals(comboAlgo.getSelectedItem());
            lblQuantum.setVisible(isRR);
            spinQuantum.setVisible(isRR);
        });

        btnBuscar.addActionListener(e -> buscarArquivo());
        btnSimular.addActionListener(e -> iniciarSimulacao());
        btnExportar.addActionListener(e -> exportarPNG());
    }

    private JPanel createStatCard(String title, JLabel valueLabel) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CARD_BG);
        card.setBorder(new EmptyBorder(10, 20, 10, 20));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblTitle.setForeground(TEXT_WHITE);

        valueLabel.setFont(new Font("Monospaced", Font.BOLD, 28));
        valueLabel.setForeground(ACCENT_BLUE);

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private JButton createStyledButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Color.BLACK);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void styleTable(JTable table) {
        table.setBackground(PANEL_DARK);
        table.setForeground(TEXT_WHITE);
        table.setRowHeight(30);
        table.setGridColor(new Color(60, 65, 75));
        table.getTableHeader().setBackground(CARD_BG);
        table.getTableHeader().setForeground(TEXT_WHITE);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
    }

    private void buscarArquivo() {
        JFileChooser chooser = new JFileChooser(new File("."));
        chooser.setDialogTitle("Selecionar apenas arquivos .txt");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Arquivos de Texto (.txt)", "txt"));

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            if (f.getName().toLowerCase().endsWith(".txt")) {
                caminhoArquivoCompleto = f.getAbsolutePath();
                lblArquivoNome.setText(f.getName());
                lblArquivoNome.setForeground(new Color(209, 154, 102));
            } else {
                JOptionPane.showMessageDialog(this, "Erro: Selecione apenas arquivos com extensão .txt",
                        "Extensão Inválida", 0);
                caminhoArquivoCompleto = "";
                lblArquivoNome.setText("Nenhum arquivo...");
                lblArquivoNome.setForeground(TEXT_WHITE);
            }
        }
    }

    private void iniciarSimulacao() {
        if (timer != null && timer.isRunning())
            timer.stop();
        if (caminhoArquivoCompleto.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Selecione o arquivo de entrada!", "Aviso", 1);
            return;
        }

        SimuladorApp.LeituraResultado lr = SimuladorApp.lerArquivo(caminhoArquivoCompleto);
        if (lr.processos() == null) {
            JOptionPane.showMessageDialog(this, "Erro: " + lr.erros(), "Erro", 0);
            return;
        }
        if (!lr.erros().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Algumas linhas foram ignoradas:\n" + String.join("\n", lr.erros()),
                    "Aviso", 1);
        }

        List<Processo> processos = lr.processos();
        String algo = (String) comboAlgo.getSelectedItem();
        SimuladorApp.ResultadoSimulacao res;

        tableModel.setRowCount(0);
        currentTimeline.clear();
        ganttPanel.setTimeline(currentTimeline);

        switch (algo) {
            case "FCFS" -> res = SimuladorApp.simular(processos, "FCFS", false,
                    Comparator.comparingInt(Processo::getTempoChegada));
            case "SJF (Não Preemptivo)" -> res = SimuladorApp.simular(processos, "SJF", false,
                    Comparator.comparingInt(Processo::getTempoExecucaoTotal));
            case "SRTF (Preemptivo)" -> res = SimuladorApp.simular(processos, "SRTF", true,
                    Comparator.comparingInt(p -> (p.getTempoExecucaoTotal() - p.getTempoCPUAcumulado())));
            case "Prioridade (Não Preemptivo)" ->
                res = SimuladorApp.simular(processos, "Prio", false, Comparator.comparingInt(Processo::getPrioridade));
            case "Prioridade (Preemptivo)" ->
                res = SimuladorApp.simular(processos, "PrioP", true, Comparator.comparingInt(Processo::getPrioridade));
            case "Round Robin" -> res = SimuladorApp.simularRR(processos, (int) spinQuantum.getValue());
            default -> res = null;
        }

        if (res != null) {
            algoSimulado = algo;
            quantumSimulado = (int) spinQuantum.getValue();

            fullTimeline = res.timeline();
            double tEsperaTot = 0, tRetornoTot = 0;

            if (res.concluidos().isEmpty()) {
                lblMediaEspera.setText("0.0 ms");
                lblMediaRetorno.setText("0.0 ms");
            } else {
                for (Processo p : res.concluidos()) {
                    tableModel.addRow(new Object[] { p.getId(), p.getTempoChegada(), p.getTempoExecucaoTotal(),
                            p.getPrioridade(), p.getTempoTotalEspera(), p.getTempoRetorno(), p.getTempoResposta() });
                    tEsperaTot += p.getTempoTotalEspera();
                    tRetornoTot += p.getTempoRetorno();
                }
                lblMediaEspera.setText(String.format("%.2f ms", tEsperaTot / res.concluidos().size()));
                lblMediaRetorno.setText(String.format("%.2f ms", tRetornoTot / res.concluidos().size()));
            }

            final int[] step = { 0 };
            timer = new Timer(30, e -> {
                if (step[0] < fullTimeline.size()) {
                    currentTimeline.add(fullTimeline.get(step[0]++));
                    ganttPanel.revalidate();
                    ganttPanel.repaint();

                    // Auto-scroll para a direita
                    SwingUtilities.invokeLater(() -> {
                        JScrollBar horizontal = scrollGantt.getHorizontalScrollBar();
                        horizontal.setValue(horizontal.getMaximum());
                    });
                } else {
                    ((Timer) e.getSource()).stop();
                }
            });
            timer.start();
        }
    }

    private void exportarPNG() {
        if (currentTimeline.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Não há dados no gráfico para exportar. Execute a simulação primeiro.",
                    "Aviso", 1);
            return;
        }
        try {
            File dir = new File("exports");
            if (!dir.exists())
                dir.mkdir();

            String nomeArq = "exports/gantt_" + algoSimulado.replace(" ", "_") + "_" + System.currentTimeMillis()
                    + ".png";
            File out = new File(nomeArq);

            // Criar imagem um pouco maior para o cabeçalho
            int headerH = 80;
            BufferedImage image = new BufferedImage(ganttPanel.getWidth(), ganttPanel.getHeight() + headerH,
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Fundo do Cabeçalho
            g.setColor(BG_DARK);
            g.fillRect(0, 0, image.getWidth(), image.getHeight());

            // Texto do Cabeçalho
            g.setColor(ACCENT_BLUE);
            g.setFont(new Font("SansSerif", Font.BOLD, 18));
            g.drawString("Simulação de Escalonamento de Processos", 20, 35);

            g.setColor(TEXT_WHITE);
            g.setFont(new Font("SansSerif", Font.PLAIN, 14));
            String info = "Algoritmo: " + algoSimulado;
            if (algoSimulado.equals("Round Robin")) {
                info += " (Quantum: " + quantumSimulado + ")";
            }
            g.drawString(info, 20, 60);

            // Desenhar o Gráfico abaixo do cabeçalho
            g.translate(0, headerH);
            ganttPanel.paint(g);
            g.dispose();

            ImageIO.write(image, "PNG", out);
            JOptionPane.showMessageDialog(this, "Gráfico exportado com sucesso!\nLocal: " + out.getAbsolutePath(),
                    "Sucesso", 1);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro na exportação: " + ex.getMessage(), "Erro", 0);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SimuladorGUI().setVisible(true));
    }

    class GanttPanel extends JPanel {
        private List<SimuladorApp.LogEvento> tl = new ArrayList<>();
        private final int BOX_W = 35;
        private final int BOX_H = 70;
        private final Map<String, Color> cores = new HashMap<>();
        private final Color[] PALETTE = {
                new Color(224, 108, 117), new Color(152, 195, 121), new Color(229, 192, 123),
                new Color(97, 175, 239), new Color(198, 120, 221), new Color(86, 182, 194)
        };

        public void setTimeline(List<SimuladorApp.LogEvento> t) {
            this.tl = t;
        }

        @Override
        public Dimension getPreferredSize() {
            // Garante que o painel cresça conforme a lista tl aumenta
            int width = Math.max(1000, tl.size() * BOX_W + 100);
            return new Dimension(width, BOX_H + 110);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int x = 40, y = 30;
            cores.put("ocioso", new Color(60, 65, 75));

            for (SimuladorApp.LogEvento evt : tl) {
                if (!cores.containsKey(evt.idProcesso())) {
                    cores.put(evt.idProcesso(), PALETTE[cores.size() % PALETTE.length]);
                }

                // Desenhar bloco com canto arredondado e gradiente
                Color base = cores.get(evt.idProcesso());
                GradientPaint grad = new GradientPaint(x, y, base, x, y + BOX_H, base.darker());
                g2.setPaint(grad);
                g2.fill(new RoundRectangle2D.Double(x, y, BOX_W - 2, BOX_H, 8, 8));

                g2.setColor(new Color(255, 255, 255, 40));
                g2.setStroke(new BasicStroke(1));
                g2.draw(new RoundRectangle2D.Double(x, y, BOX_W - 2, BOX_H, 8, 8));

                // Desenhar Marcador de Tempo (Tick)
                g2.setColor(new Color(171, 178, 191, 100));
                g2.drawLine(x, y + BOX_H, x, y + BOX_H + 5);

                // Texto do Tempo (Mais legível e alinhado)
                g2.setColor(TEXT_WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, 12));

                String tempoStr = String.valueOf(evt.tempo());
                FontMetrics fm = g2.getFontMetrics();
                int textX = x - (fm.stringWidth(tempoStr) / 2); // Centralizar no tick
                g2.drawString(tempoStr, textX, y + BOX_H + 22);

                if (!evt.idProcesso().equals("ocioso")) {
                    g2.setColor(Color.BLACK);
                    g2.setFont(new Font("SansSerif", Font.BOLD, 13));
                    // Centralizar ID do processo no bloco
                    int idX = x + (BOX_W / 2) - (g2.getFontMetrics().stringWidth(evt.idProcesso()) / 2);
                    g2.drawString(evt.idProcesso(), idX, y + 40);
                }
                x += BOX_W;

                // Desenhar o último marcador (fim da timeline)
                if (tl.indexOf(evt) == tl.size() - 1) {
                    int fimX = x;
                    g2.setColor(new Color(171, 178, 191, 100));
                    g2.drawLine(fimX, y + BOX_H, fimX, y + BOX_H + 5);
                    String fimStr = String.valueOf(evt.tempo() + 1);
                    int textFimX = fimX - (g2.getFontMetrics().stringWidth(fimStr) / 2);
                    g2.setColor(TEXT_WHITE);
                    g2.drawString(fimStr, textFimX, y + BOX_H + 22);
                }
            }
        }
    }
}
