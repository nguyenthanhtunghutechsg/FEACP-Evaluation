package CLHMiner;

import java.io.IOException;

public class Main {

	public static void main(String[] args) throws IOException {

		String TaxonomyPath = "chesstaxonomy.txt";
		String inputPath = "chess.txt";
		// String TaxonomyPath = "liquorTaxonomy.txt";
		// String inputPath = "liquor.txt";
		CLH_Miner cl = new CLH_Miner();
		double minU = 1900000;
		// CLH_MinerTestP cl = new CLH_MinerTestP();
		 //pCLH_Miner cl = new pCLH_Miner();
		for (int i = 0; i < 5; i++) {
			System.gc();
			cl.runAlgorithm((int)minU , inputPath, "", TaxonomyPath);
			cl.printStats();
			minU-=50000;
			
		}

		

//2088282/2150177
	}
}
