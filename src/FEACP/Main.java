package FEACP;

import java.io.IOException;


public class Main {

	public static void main(String[] args) throws IOException {

//		String TaxonomyPath = "connectTaxonomy.txt";
//		String inputPath = "connect.txt";
		String TaxonomyPath = "chesstaxonomy.txt";
		String inputPath = "chess.txt";
		AlgoFEACP cl = new AlgoFEACP();
		// CLH_MinerTestP cl = new CLH_MinerTestP();
		// pCLH_Miner cl = new pCLH_Miner();
		double minU = 2100000;
		// CLH_MinerTestP cl = new CLH_MinerTestP();
		 //pCLH_Miner cl = new pCLH_Miner();
		for (int i = 0; i < 9; i++) {
			System.gc();
			cl.runAlgorithm((int)minU , inputPath, "", TaxonomyPath, Integer.MAX_VALUE);
			cl.printStats();
			minU-=50000;
			
		}

//2088282/2150177
	}
}
