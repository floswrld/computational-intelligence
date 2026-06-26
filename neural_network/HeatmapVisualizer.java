import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;
import java.util.List;

public class HeatmapVisualizer extends JPanel {

    private int width, height;
    private int startX, startY;
    private int goalX, goalY;
    private double[][] maxQValues;
    private boolean[][] obstacle;
    private int[][] policy;
    private List<int[]> path;

    private int guessX, guessY;

    private JFrame frame;

    public HeatmapVisualizer(int w, int h, int startX, int startY, int goalX, int goalY, boolean[][] obstacle) {
        this.width = w;
        this.height = h;
        this.startX = startX;
        this.startY = startY;
        this.goalX = goalX;
        this.goalY = goalY;
        this.obstacle = obstacle;
        this.maxQValues = new double[w][h];
        this.policy = null;
        this.path = null;

        SwingUtilities.invokeLater(() -> {
            frame = new JFrame("Q-Learning Heatmap");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(this);
            frame.setSize(620, 640);
            frame.setVisible(true);
        });
    }

    public void update(double[][][] Q) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                double best = Q[x][y][0];
                for (int a = 1; a < Q[x][y].length; a++) best = Math.max(best, Q[x][y][a]);
                maxQValues[x][y] = best;
            }
        }
        SwingUtilities.invokeLater(this::repaint);
    }

    public void markCell(int[][] cell){

        SwingUtilities.invokeLater(this::repaint);
    }

    public void setPolicy(int[][] policy) {
        this.policy = policy;
        SwingUtilities.invokeLater(this::repaint);
    }

    public void setPath(List<int[]> path) {
        this.path = path;
        SwingUtilities.invokeLater(this::repaint);
    }

    private double[] colorRange() {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (obstacle[x][y]) continue;
                min = Math.min(min, maxQValues[x][y]);
                max = Math.max(max, maxQValues[x][y]);
            }
        }
        if (!(max > min)) { min = 0; max = 1; }
        return new double[]{min, max};
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int cellWidth = getWidth() / width;
        int cellHeight = getHeight() / height;

        double[] range = colorRange();
        double min = range[0], max = range[1];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {

                int px = x * cellWidth;
                int py = y * cellHeight;

                if (obstacle[x][y]) {
                    g2.setColor(new Color(40, 40, 40));
                    g2.fillRect(px, py, cellWidth, cellHeight);
                } else {
                    float ratio = (float) ((maxQValues[x][y] - min) / (max - min));
                    ratio = Math.max(0f, Math.min(1f, ratio));
                    g2.setColor(getColor(ratio));
                    g2.fillRect(px, py, cellWidth, cellHeight);
                }

                g2.setColor(new Color(80, 80, 80));
                g2.drawRect(px, py, cellWidth, cellHeight);

                if (!obstacle[x][y]) {
                    g2.setColor(Color.WHITE);
                    String text = String.format("%.1f", maxQValues[x][y]);
                    g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
                    g2.drawString(text, px + 4, py + 14);
                }
            }
        }

        if (path != null && path.size() > 1) {
            g2.setStroke(new BasicStroke(4f));
            g2.setColor(new Color(255, 255, 255, 200));
            for (int i = 0; i < path.size() - 1; i++) {
                int[] a = path.get(i);
                int[] b = path.get(i + 1);
                double ax = a[0] * cellWidth + cellWidth / 2.0;
                double ay = a[1] * cellHeight + cellHeight / 2.0;
                double bx = b[0] * cellWidth + cellWidth / 2.0;
                double by = b[1] * cellHeight + cellHeight / 2.0;
                g2.draw(new Line2D.Double(ax, ay, bx, by));
            }
        }

        if (policy != null) {
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(1.5f));
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    if (obstacle[x][y] || policy[x][y] < 0) continue;
                    if (x == startX && y == startY) continue;
                    drawArrow(g2, policy[x][y], x * cellWidth, y * cellHeight, cellWidth, cellHeight);
                }
            }
        }

        int sx = startX * cellWidth, sy = startY * cellHeight;
        g2.setColor(new Color(0, 200, 0));
        g2.setStroke(new BasicStroke(3f));
        g2.drawRect(sx + 2, sy + 2, cellWidth - 4, cellHeight - 4);
        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        g2.drawString("S", sx + cellWidth - 16, sy + cellHeight - 6);

        int gx = goalX * cellWidth, gy = goalY * cellHeight;
        g2.setColor(new Color(255, 215, 0));
        g2.fillRect(gx, gy, cellWidth, cellHeight);
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("SansSerif", Font.BOLD, 18));
        g2.drawString("G", gx + cellWidth / 2 - 6, gy + cellHeight / 2 + 6);
    }

    private void drawArrow(Graphics2D g2, int action, int px, int py, int cw, int ch) {
        int cx = px + cw / 2;
        int cy = py + ch / 2;
        int len = Math.min(cw, ch) / 3;

        int ex = cx, ey = cy;
        switch (action) {
            case 0: ey = cy - len; break;
            case 1: ey = cy + len; break;
            case 2: ex = cx - len; break;
            case 3: ex = cx + len; break;
        }
        g2.drawLine(cx, cy, ex, ey);

        int head = 4;
        switch (action) {
            case 0: g2.drawLine(ex, ey, ex - head, ey + head); g2.drawLine(ex, ey, ex + head, ey + head); break;
            case 1: g2.drawLine(ex, ey, ex - head, ey - head); g2.drawLine(ex, ey, ex + head, ey - head); break;
            case 2: g2.drawLine(ex, ey, ex + head, ey - head); g2.drawLine(ex, ey, ex + head, ey + head); break;
            case 3: g2.drawLine(ex, ey, ex - head, ey - head); g2.drawLine(ex, ey, ex - head, ey + head); break;
        }
    }

    private Color getColor(float ratio) {
        ratio = Math.max(0f, Math.min(1f, ratio));
        return new Color(ratio, 0f, 1f - ratio);
    }
}
