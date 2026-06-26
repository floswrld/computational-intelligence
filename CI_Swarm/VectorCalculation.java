
public class VectorCalculation {
	static double truncate(double x, double y) {
	
		if (y < 0)
			System.out.println("Mistake truncate");
		if (x > 0)
			return Math.min(x, y);
		else
			return Math.max(x, -y);
	}

	static double[] normalize(double[] x) {
		double[] res = new double[2];
		double norm = Math.sqrt(Math.pow(x[0], 2) + Math.pow(x[1], 2));
		res[0] = x[0];
		res[1] = x[1];
		if (norm > 1e-8) {
			res[0] = x[0] / norm;
			res[1] = x[1] / norm;
		}

		return res;
	}

	static double[] truncate(double[] x, double y) {
		if (y < 0)
			System.out.println("Mistake truncate");
		double[] res = normalize(x);
		res[0] = res[0] * truncate(length(x), y);
		res[1] = res[1] * truncate(length(x), y);
		return res;
	}

	static double length(double[] x) {
		double res = Math.sqrt(Math.pow(x[0], 2) + Math.pow(x[1], 2));
		return res;
	}

	static double angle(double[] v1) {

		double[] k = new double[2];
		double w;

		k[0] = 1;
		k[1] = 0;
		w = angle(k, v1);
		if (v1[1] < 0)
			w = 2 * Math.PI - w;
		return w;
	}

	static double angle(double[] v1, double[] v2) {
		double betrag_v1 = Math.sqrt(Math.pow(v1[0], 2) + Math.pow(v1[1], 2));
		double betrag_v2 = Math.sqrt(Math.pow(v2[0], 2) + Math.pow(v2[1], 2));
		double winkelRad;//angleRadiant
		double skalPro;

		if (betrag_v1 == 0 || betrag_v2 == 0) {
			winkelRad = 0;
		} else {
			skalPro = (v1[0] * v2[0]) + (v1[1] * v2[1]);
			winkelRad = skalPro / (betrag_v1 * betrag_v2);
			if (winkelRad > 1)
				winkelRad = 1;
			if (winkelRad < -1)
				winkelRad = -1;
			winkelRad = Math.acos(winkelRad);
		}
		return winkelRad;
	}
	static double[] dist(double[] pkt, double[] ort1, double[] ort2) {
		double[] abstandsPkt = new double[2];
		abstandsPkt[0] = 0;
		abstandsPkt[1] = 0;

		double dist;
		double winkel1;
		double winkel2;

		double[] richtung1 = new double[2];
		double[] richtung2 = new double[2];

		richtung1[0] = ort2[0] - ort1[0];
		richtung1[1] = ort2[1] - ort1[1];
		richtung2[0] = pkt[0] - ort1[0];
		richtung2[1] = pkt[1] - ort1[1];
		winkel1 = angle(richtung1, richtung2);
		richtung1[0] = ort1[0] - ort2[0];
		richtung1[1] = ort1[1] - ort2[1];
		richtung2[0] = pkt[0] - ort2[0];
		richtung2[1] = pkt[1] - ort2[1];
		winkel2 = angle(richtung1, richtung2);

		if (winkel1 >= Math.PI / 2) {
			abstandsPkt[0] = ort1[0];
			abstandsPkt[1] = ort1[1];
		} else if (winkel2 >= Math.PI / 2) {
			abstandsPkt[0] = ort2[0];
			abstandsPkt[1] = ort2[1];
		} else {
			richtung1[0] = ort2[0] - ort1[0];
			richtung1[1] = ort2[1] - ort1[1];
			richtung2[0] = pkt[0] - ort1[0];
			richtung2[1] = pkt[1] - ort1[1];
			winkel1 = angle(richtung1, richtung2);
			dist = length(richtung2);
			double lot = dist * Math.cos(winkel1);
			double[] lotPkt = normalize(richtung1);
			abstandsPkt[0] = ort1[0] + lot * lotPkt[0];
			abstandsPkt[1] = ort1[1] + lot * lotPkt[1];
		}

		return abstandsPkt;
	}

}
