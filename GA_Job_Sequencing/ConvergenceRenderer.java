import java.awt.*;
import java.util.List;

public class ConvergenceRenderer {

	public static void paint(Graphics2D g, int w, int h, List<int[]> data){
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, w, h);

		int L = 72, R = 24, T = 30, B = 44;

		g.setColor(new Color(40, 40, 40));
		g.setFont(g.getFont().deriveFont(Font.BOLD, 13f));
		g.drawString("Convergence: best fitness per iteration", L, 22);

		if(data == null || data.size() < 2){
			g.setColor(Color.GRAY);
			g.setFont(g.getFont().deriveFont(12f));
			g.drawString("Waiting for data...", L, h / 2);
			return;
		}

		int minF = Integer.MAX_VALUE, maxF = Integer.MIN_VALUE, maxIt = 1;
		for(int[] d : data){
			minF = Math.min(minF, d[1]);
			maxF = Math.max(maxF, d[1]);
			maxIt = Math.max(maxIt, d[0]);
		}
		if(maxF == minF) maxF = minF + 1;

		int pw = w - L - R, ph = h - T - B;

		// grid + labels
		g.setFont(g.getFont().deriveFont(10f));
		for(int i = 0; i <= 4; i++){
			int val = maxF - (maxF - minF) * i / 4;
			int y = T + ph * i / 4;
			g.setColor(new Color(234, 234, 236));
			g.drawLine(L, y, L + pw, y);
			g.setColor(new Color(110, 110, 110));
			g.drawString(String.valueOf(val), 8, y + 4);
		}
		for(int i = 0; i <= 5; i++){
			int it = maxIt * i / 5;
			int x = L + pw * i / 5;
			g.setColor(new Color(110, 110, 110));
			g.drawString(String.valueOf(it), x - 10, T + ph + 16);
		}

		// axes
		g.setColor(new Color(170, 170, 170));
		g.drawLine(L, T, L, T + ph);
		g.drawLine(L, T + ph, L + pw, T + ph);

		// curve
		g.setColor(new Color(30, 110, 200));
		g.setStroke(new BasicStroke(2f));
		int px = -1, py = -1;
		for(int[] d : data){
			int x = L + (int)((long) d[0] * pw / maxIt);
			int y = T + (int)((long)(maxF - d[1]) * ph / (maxF - minF));
			if(px >= 0) g.drawLine(px, py, x, y);
			px = x; py = y;
		}

		int[] last = data.get(data.size() - 1);
		g.setColor(new Color(200, 50, 50));
		g.fillOval(px - 3, py - 3, 6, 6);

		g.setColor(new Color(60, 60, 60));
		g.setFont(g.getFont().deriveFont(Font.BOLD, 11f));
		String info = "Start " + data.get(0)[1] + "   Ende " + last[1];
		g.drawString(info, L + pw - g.getFontMetrics().stringWidth(info), T + 14);
	}
}
