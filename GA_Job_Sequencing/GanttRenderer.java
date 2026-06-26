import java.awt.*;

public class GanttRenderer {

	public static final int LEFT = 64, TOP = 46, ROW = 46, GAP = 10, BOTTOM = 34;

	public static int contentWidth(int makespan, int pxPerUnit){
		return LEFT + makespan * pxPerUnit + 40;
	}

	public static int contentHeight(int m){
		return TOP + m * ROW + BOTTOM;
	}

	public static void paint(Graphics2D g, int w, int h, int[] perm, int[][] start, int[][] end, int m, int pxPerUnit){
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, w, h);

		if(perm == null || perm.length == 0){
			g.setColor(Color.GRAY);
			g.setFont(g.getFont().deriveFont(14f));
			g.drawString("Nothing calculated yet. Please 'Start'.", 20, 30);
			return;
		}

		int n = perm.length;
		int makespan = end[n - 1][m - 1];

		g.setColor(new Color(40, 40, 40));
		g.setFont(g.getFont().deriveFont(Font.BOLD, 13f));
		g.drawString("Gantt-Diagram  (Machines x Time)", LEFT, 22);

		// time grid + axis labels
		int step = niceStep(makespan);
		g.setFont(g.getFont().deriveFont(Font.PLAIN, 10f));
		for(int t = 0; t <= makespan; t += step){
			int x = LEFT + t * pxPerUnit;
			g.setColor(new Color(232, 232, 234));
			g.drawLine(x, TOP, x, TOP + m * ROW - GAP);
			g.setColor(new Color(120, 120, 120));
			g.drawString(String.valueOf(t), x - 4, TOP + m * ROW - GAP + 16);
		}

		// machine rows + labels
		for(int k = 0; k < m; k++){
			int y = TOP + k * ROW;
			g.setColor(new Color(246, 247, 249));
			g.fillRect(LEFT, y, makespan * pxPerUnit, ROW - GAP);
			g.setColor(new Color(70, 70, 70));
			g.setFont(g.getFont().deriveFont(Font.BOLD, 11f));
			g.drawString("M" + (k + 1), 16, y + (ROW - GAP) / 2 + 4);
		}

		// job bars
		g.setFont(g.getFont().deriveFont(Font.PLAIN, 10f));
		FontMetrics fm = g.getFontMetrics();
		for(int i = 0; i < n; i++){
			int job = perm[i];
			Color c = Color.getHSBColor((float) job / n, 0.55f, 0.92f);
			for(int k = 0; k < m; k++){
				int s = start[i][k], e = end[i][k];
				int x = LEFT + s * pxPerUnit, y = TOP + k * ROW;
				int bw = Math.max(1, (e - s) * pxPerUnit), bh = ROW - GAP;
				g.setColor(c);
				g.fillRect(x, y, bw, bh);
				g.setColor(c.darker());
				g.drawRect(x, y, bw, bh);
				if(bw >= 16){
					g.setColor(new Color(25, 25, 25));
					String lbl = String.valueOf(job);
					g.drawString(lbl, x + (bw - fm.stringWidth(lbl)) / 2, y + (bh + fm.getAscent()) / 2 - 2);
				}
			}
		}
	}

	private static int niceStep(int span){
		if(span <= 0) return 1;
		int target = span / 12;
		int[] nice = {1, 2, 5, 10, 20, 25, 50, 100, 200, 250, 500, 1000, 2000, 5000};
		for(int s : nice) if(s >= target) return s;
		return 10000;
	}
}
