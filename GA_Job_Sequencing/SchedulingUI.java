import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SchedulingUI extends JFrame {

	private static final String DATA_DIR =
			System.getProperty("user.home") + "/CI/GA_Job_Sequencing/data/";

	private static final String[] INSTANCES = {
			DATA_DIR + "datenCustomer_5_3.txt",
			DATA_DIR + "daten3ACustomer_200_10.txt",
			DATA_DIR + "daten3BCustomer_200_20.txt",
			DATA_DIR + "daten4ACustomer_200_5.txt",
			DATA_DIR + "daten4BCustomer_200_5.txt"
	};

	private JComboBox<String> instanceBox;
	private JTextField iterField;
	private JButton runButton, cancelButton;
	private JLabel statusLabel;
	private JTextField sequenceField;
	private JSlider zoomSlider;
	private GanttPanel ganttPanel;
	private ConvergencePanel convPanel;

	public SchedulingUI(){
		super("Just-In-Sequence Scheduling - GA Visualization");
		setDefaultCloseOperation(EXIT_ON_CLOSE);

		instanceBox  = new JComboBox<>(INSTANCES);
		instanceBox.setSelectedItem("data/daten3ACustomer_200_10.txt");
		iterField    = new JTextField("10000", 6);
		runButton    = new JButton("Start");
		cancelButton = new JButton("Cancel");
		cancelButton.setEnabled(false);
		statusLabel  = new JLabel("Ready.");
		zoomSlider   = new JSlider(1, 30, 6);
		zoomSlider.setPreferredSize(new Dimension(120, 24));

		JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
		top.add(new JLabel("Instance:"));      top.add(instanceBox);
		top.add(new JLabel("Iterations:"));  top.add(iterField);
		top.add(runButton);                   top.add(cancelButton);
		top.add(new JLabel("Zoom:"));         top.add(zoomSlider);

		ganttPanel = new GanttPanel();
		convPanel  = new ConvergencePanel();

		JScrollPane ganttScroll = new JScrollPane(ganttPanel,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		ganttScroll.getHorizontalScrollBar().setUnitIncrement(24);

		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab("Gantt-Diagram", ganttScroll);
		tabs.addTab("Convergence", convPanel);

		sequenceField = new JTextField();
		sequenceField.setEditable(false);
		JPanel seqRow = new JPanel(new BorderLayout(6, 0));
		seqRow.add(new JLabel(" Sequence: "), BorderLayout.WEST);
		seqRow.add(sequenceField, BorderLayout.CENTER);

		JPanel south = new JPanel(new BorderLayout());
		south.add(statusLabel, BorderLayout.NORTH);
		south.add(seqRow, BorderLayout.SOUTH);

		setLayout(new BorderLayout());
		add(top, BorderLayout.NORTH);
		add(tabs, BorderLayout.CENTER);
		add(south, BorderLayout.SOUTH);

		runButton.addActionListener(e -> startRun());
		cancelButton.addActionListener(e -> {
			GA.cancelRequested = true;
			cancelButton.setEnabled(false);
			statusLabel.setText("Cancelling...");
		});
		zoomSlider.addChangeListener(e -> ganttPanel.setZoom(zoomSlider.getValue()));

		setSize(1150, 720);
		setLocationRelativeTo(null);
	}

	private void startRun(){
		int iters;
		try {
			iters = Integer.parseInt(iterField.getText().trim());
			if(iters < 1) throw new NumberFormatException();
		} catch(Exception ex){
			JOptionPane.showMessageDialog(this, "Right iteration number please");
			return;
		}

		String instance = (String) instanceBox.getSelectedItem();
		try {
			Problem.readInstance(instance);
		} catch(Exception ex){
			JOptionPane.showMessageDialog(this, ex.getMessage());
			return;
		}

		convPanel.reset();
		ganttPanel.setSchedule(null, null, null, 0);
		runButton.setEnabled(false);
		cancelButton.setEnabled(true);
		statusLabel.setText("Running... (" + Problem.n + " Jobs, " + Problem.m + " Machines)");

		new GAWorker(iters).execute();
	}

	// runs the GA off the EDT and streams progress back
	private class GAWorker extends SwingWorker<Individual, int[]> {
		private final int iters;
		private final int every;

		GAWorker(int iters){
			this.iters = iters;
			this.every = Math.max(1, iters / 400);
		}

		protected Individual doInBackground(){
			return GA.runGA(iters, (it, f) -> {
				if(it == 1 || it % every == 0 || it == iters) publish(new int[]{it, f});
			});
		}

		protected void process(List<int[]> chunks){
			for(int[] p : chunks) convPanel.add(p[0], p[1]);
			convPanel.repaint();
			int[] last = chunks.get(chunks.size() - 1);
			statusLabel.setText("Iteration " + last[0] + " / " + iters + "  |  best Fitness " + last[1]);
		}

		protected void done(){
			try {
				Individual best = get();
				int n = Problem.n, m = Problem.m;
				int[] perm = best.perm;
				int[][] end = Problem.endTimes(perm);
				int[][] start = new int[n][m];
				for(int i = 0; i < n; i++)
					for(int k = 0; k < m; k++)
						start[i][k] = end[i][k] - Problem.time[perm[i]][k];

				ganttPanel.setSchedule(perm, start, end, m);
				int ms = end[n - 1][m - 1];
				statusLabel.setText("Done  |  Fitness " + best.fitness + "  |  Makespan " + ms
						+ "  |  " + n + " Jobs, " + m + " Machines");
				sequenceField.setText(sequence(perm));
			} catch(Exception ex){
				statusLabel.setText("Error: " + ex.getMessage());
			}
			runButton.setEnabled(true);
			cancelButton.setEnabled(false);
		}

		private String sequence(int[] p){
			StringBuilder sb = new StringBuilder();
			for(int i = 0; i < p.length; i++){
				if(i > 0) sb.append('-');
				sb.append(p[i]);
			}
			return sb.toString();
		}
	}

	// ---- panels delegate the actual drawing to the renderers ----

	static class GanttPanel extends JPanel {
		int[] perm; int[][] start; int[][] end; int m; int makespan; int pxPerUnit = 6;

		GanttPanel(){ setBackground(Color.WHITE); }

		void setSchedule(int[] perm, int[][] start, int[][] end, int m){
			this.perm = perm; this.start = start; this.end = end; this.m = m;
			this.makespan = (perm == null || perm.length == 0) ? 0 : end[perm.length - 1][m - 1];
			revalidate(); repaint();
		}

		void setZoom(int px){ this.pxPerUnit = px; revalidate(); repaint(); }

		public Dimension getPreferredSize(){
			if(perm == null) return new Dimension(800, 320);
			return new Dimension(GanttRenderer.contentWidth(makespan, pxPerUnit), GanttRenderer.contentHeight(m));
		}

		protected void paintComponent(Graphics g){
			super.paintComponent(g);
			GanttRenderer.paint((Graphics2D) g, getWidth(), getHeight(), perm, start, end, m, pxPerUnit);
		}
	}

	static class ConvergencePanel extends JPanel {
		final List<int[]> data = new ArrayList<>();

		ConvergencePanel(){ setBackground(Color.WHITE); }

		void reset(){ data.clear(); repaint(); }
		void add(int it, int f){ data.add(new int[]{it, f}); }

		protected void paintComponent(Graphics g){
			super.paintComponent(g);
			ConvergenceRenderer.paint((Graphics2D) g, getWidth(), getHeight(), data);
		}
	}

	public static void main(String[] args){
		SwingUtilities.invokeLater(() -> new SchedulingUI().setVisible(true));
	}
}
