package FEACPLu;

import java.io.BufferedWriter;
import java.io.FileWriter;
//import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import java.lang.management.*;

/**
 * This is an implementation of the EFIM algorithm for mining high-utility
 * itemsets from a transaction database. More information on the EFIM algorithm
 * can be found in that paper: <br\>
 *
 * @author Souleymane Zida, Philippe Fournier-Viger using some code by Alan
 *         Souza
 */
public class AlgoEFIM {

	/** the set of high-utility itemsets */
	// private Itemsets highUtilityItemsets;

	/** object to write the output file */
	BufferedWriter writer = null;

	/** the number of high-utility itemsets found (for statistics) */
	private int patternCount;
	int countHUI = 0;
	/** the start time and end time of the last algorithm execution */
	long startTimestamp;
	long endTimestamp;

	/** the minutil threshold */
	long minUtil;

	/** if this variable is set to true, some debugging information will be shown */
	final boolean DEBUG = false;

	/**
	 * The following variables are the utility-bins array // Recall that each bucket
	 * correspond to an item
	 */
	/** utility bin array for sub-tree utility */
	private double[] utilityBinArraySU;
	/** utility bin array for local utility */
	private double[] utilityBinArrayLU;

	/** a temporary buffer */
	private int[] temp = new int[500];

	/** The total time spent for performing intersections */
	long timeIntersections;
	/** The total time spent for performing database reduction */
	long timeDatabaseReduction;
	/** The total time spent for identifying promising items */
	long timeIdentifyPromisingItems;
	/** The total time spent for sorting */
	long timeSort;
	/** The total time spent for binary search */
	long timeBinarySearch;

	/** an array that map an old item name to its new name */
	int[] oldNameToNewNames;
	/** an array that map a new item name to its old name */
	int[] newNamesToOldNames;
	/** the number of new items */
	int newItemCount;

	/** if true, transaction merging will be performed by the algorithm */
	boolean activateTransactionMerging;

	/** A parameter for transaction merging */
	final int MAXIMUM_SIZE_MERGING = 1000;

	/** number of times a transaction was read */
	long transactionReadingCount;
	/** number of merges */
	long mergeCount;

	/** number of itemsets from the search tree that were considered */
	private long candidateCount;

	/** If true, sub-tree utility pruning will be performed */
	private boolean activateSubtreeUtilityPruning;
	TaxonomyTree taxonomy;

	/**
	 * Constructor
	 */
	public AlgoEFIM() {

	}

	public static long timeProject = 0;

	/**
	 * Run the algorithm
	 * 
	 * @param minUtil                       the minimum utility threshold (a
	 *                                      positive integer)
	 * @param inputPath                     the input file path
	 * @param outputPath                    the output file path to save the result
	 *                                      or null if to be kept in memory
	 * @param activateTransactionMerging
	 * @param activateSubtreeUtilityPruning
	 * @param maximumTransactionCount
	 * @return the itemsets or null if the user choose to save to file
	 * @throws IOException if exception while reading/writing to file
	 */
	public void runAlgorithm(int minUtil, String inputPath, String outputPath, String TaxonomyPath,
			int maximumTransactionCount) throws IOException {

		// reset variables for statistics
		mergeCount = 0;
		transactionReadingCount = 0;
		timeIntersections = 0;
		timeDatabaseReduction = 0;

		// save parameters about activating or not the optimizations

		// read the input file
		Dataset dataset = new Dataset(inputPath, maximumTransactionCount, TaxonomyPath);
		taxonomy = dataset.taxonomy;
		// record the start time
		startTimestamp = System.currentTimeMillis();

		// save minUtil value selected by the user
		this.minUtil = minUtil;

		// if the user choose to save to file
		// create object for writing the output file

		// reset the number of itemset found
		patternCount = 0;

		// reset the memory usage checking utility
		MemoryLogger.getInstance().reset();

		// if in debug mode, show the initial database in the console
		if (DEBUG) {
			System.out.println("===== Initial database === ");
			System.out.println(dataset.toString());
		}

		// if the user choose to save to memory
		// writer = null;
		// this.highUtilityItemsets = new Itemsets("Itemsets");

		// Scan the database using utility-bin array to calculate the TWU
		// of each item
		useUtilityBinArrayToCalculateLocalUtilityFirstTime(dataset);

		// if in debug mode, show the TWU calculated using the utility-bin array
		if (DEBUG) {
			System.out.println("===== TWU OF SINGLE ITEMS === ");
			for (int i = 1; i < utilityBinArrayLU.length; i++) {
				System.out.println("item : " + i + " twu: " + utilityBinArrayLU[i]);
			}
			System.out.println();
		}

		// Now, we keep only the promising items (those having a twu >= minutil)
		List<Integer> itemsToKeep = new ArrayList<Integer>();
		for (int j = 1; j < utilityBinArrayLU.length; j++) {
			if (utilityBinArrayLU[j] >= minUtil) {
				itemsToKeep.add(j);
			}
		}

		// Sort promising items according to the increasing order of TWU
		insertionSort(itemsToKeep, utilityBinArrayLU, taxonomy);

		// Rename promising items according to the increasing order of TWU.
		// This will allow very fast comparison between items later by the algorithm
		// This structure will store the new name corresponding to each old name
		oldNameToNewNames = new int[dataset.getMaxItem() + 1];
		// This structure will store the old name corresponding to each new name
		newNamesToOldNames = new int[dataset.getMaxItem() + 1];
		// We will now give the new names starting from the name "1"
		int currentName = 1;
		// For each item in increasing order of TWU
		for (int j = 0; j < itemsToKeep.size(); j++) {
			// get the item old name
			int item = itemsToKeep.get(j);
			// give it the new name
			oldNameToNewNames[item] = currentName;
			// remember its old name
			newNamesToOldNames[currentName] = item;
			// replace its old name by the new name in the list of promising items
			itemsToKeep.set(j, currentName);
			// increment by one the current name so that
			currentName++;
		}

		// remember the number of promising item
		newItemCount = itemsToKeep.size();
		// initialize the utility-bin array for counting the subtree utility
		utilityBinArraySU = new double[newItemCount + 1];

		// if in debug mode, print to the old names and new names to the console
		// to check if they are correct
		if (DEBUG) {
			System.out.println(itemsToKeep);
			System.out.println(Arrays.toString(oldNameToNewNames));
			System.out.println(Arrays.toString(newNamesToOldNames));
		}

		// We now loop over each transaction from the dataset
		// to remove unpromising items
		for (int i = 0; i < dataset.getTransactions().size(); i++) {
			// Get the transaction
			Transaction transaction = dataset.getTransactions().get(i);

			// Remove unpromising items from the transaction and at the same time
			// rename the items in the transaction according to their new names
			// and sort the transaction by increasing TWU order
			transaction.removeUnpromisingItems(oldNameToNewNames, taxonomy);
		}

		// Now we will sort transactions in the database according to the proposed
		// total order on transaction (the lexicographical order when transactions
		// are read backward).
		long timeStartSorting = System.currentTimeMillis();
		// We only sort if transaction merging is activated

		// =======================REMOVE EMPTY TRANSACTIONS==========================
		// After removing unpromising items, it may be possible that some transactions
		// are empty. We will now remove these transactions from the database.
		// for each transaction
		for (int i = 0; i < dataset.getTransactions().size(); i++) {
			// if the transaction length is 0, increase the number of empty transactions
			Transaction transaction = dataset.getTransactions().get(i);
			if (transaction.items.length == 0 && transaction.parentsInTransaction.size() == 0) {
				dataset.transactions.remove(transaction);
			}
		}

		timeSort = System.currentTimeMillis() - timeStartSorting;

		// if in debug mode, print the database after sorting and removing promising
		// items
		if (DEBUG) {
			System.out.println("===== Database without unpromising items and sorted by TWU increasing order === ");
			System.out.println(dataset.toString());
		}

		// Use an utility-bin array to calculate the sub-tree utility of each item
		useUtilityBinArrayToCalculateSubtreeUtilityFirstTime(dataset);

		// Calculate the set of items that pass the sub-tree utility pruning
		// condition
		
		backtrackingEFIM(dataset.getTransactions(), itemsToKeep, itemsToKeep, 0);

		// record the end time
		endTimestamp = System.currentTimeMillis();

		// close the output file
		if (writer != null) {
			writer.close();
		}

		// printPeakHeapUsage();
		// check the maximum memory usage
		MemoryLogger.getInstance().checkMemory();
		printPeakHeapUsage();
		// return the set of high-utility itemsets

	}

	/**
	 * Implementation of Insertion sort for sorting a list of items by increasing
	 * order of TWU. This has an average performance of O(n log n)
	 * 
	 * @param items list of integers to be sorted
	 * @param items list the utility-bin array indicating the TWU of each item.
	 */
	public static void insertionSort(List<Integer> items, double[] utilityBinArrayTWU, TaxonomyTree taxonomy) {
		// the following lines are simply a modified an insertion sort

		for (int j = 1; j < items.size(); j++) {
			Integer itemJ = items.get(j);
			int i = j - 1;
			Integer itemI = items.get(i);
			double comparison = taxonomy.getMapItemToTaxonomyNode().get(itemI).getLevel()
					- taxonomy.getMapItemToTaxonomyNode().get(itemJ).getLevel();
			if (comparison == 0) {
				comparison = (double) (utilityBinArrayTWU[itemI] - utilityBinArrayTWU[itemJ]);
			}
			while (comparison > 0) {
				items.set(i + 1, itemI);
				i--;
				if (i < 0) {
					break;
				}
				itemI = items.get(i);
				comparison = taxonomy.getMapItemToTaxonomyNode().get(itemI).getLevel()
						- taxonomy.getMapItemToTaxonomyNode().get(itemJ).getLevel();
				// if the twu is equal, we use the lexicographical order to decide whether i is
				// greater
				// than j or not.
				if (comparison == 0) {
					comparison = (double) (utilityBinArrayTWU[itemI] - utilityBinArrayTWU[itemJ]);
				}
			}
			items.set(i + 1, itemJ);
		}
	}

	/**
	 * Recursive method to find all high-utility itemsets
	 * 
	 * @param the            list of transactions containing the current prefix P
	 * @param itemsToKeep    the list of secondary items in the p-projected database
	 * @param itemsToExplore the list of primary items in the p-projected database
	 * @param the            current prefixLength
	 * @throws IOException if error writing to output file
	 */
	private void backtrackingEFIM(List<Transaction> transactionsOfP, List<Integer> itemsToKeep,
			List<Integer> itemsToExplore, int prefixLength) throws IOException {

		// update the number of candidates explored so far
		candidateCount += itemsToExplore.size();

		// ======== for each frequent item e =============
		for (int x = 0; x < itemsToExplore.size(); x++) {

			Integer e = itemsToExplore.get(x);

			// ========== PERFORM INTERSECTION =====================
			// Calculate transactions containing P U {e}
			// At the same time project transactions to keep what appears after "e"
			List<Transaction> transactionsPe = new ArrayList<Transaction>();

			// variable to calculate the utility of P U {e}
			double utilityPe = 0;

			// For merging transactions, we will keep track of the last transaction read
			// and the number of identical consecutive transactions

			// this variable is to record the time for performing intersection
			long timeFirstIntersection = System.currentTimeMillis();
			// For each transaction
			// Variables low and high for binary search
			
			if (taxonomy.getMapItemToTaxonomyNode().get(newNamesToOldNames[e]).getChildren().size() == 0) {
				for (Transaction transaction : transactionsOfP) {
					// Increase the number of transaction read
					transactionReadingCount++;

					// To record the time for performing binary searh
				

					// we remember the position where e appears.
					// we will call this position an "offset"

					int positionE = -1;
					int low = 0;
					int high = transaction.items.length - 1;

					// perform binary search to find e in the transaction
					while (high >= low) {
						int middle = (low + high) >>> 1; // divide by 2
						if (transaction.items[middle] < e) {
							low = middle + 1;
						} else if (transaction.items[middle] == e) {
							positionE = middle;
							break;
						} else {
							high = middle - 1;
						}
					}
					if (positionE > -1) {
						Transaction projectedTransaction = new Transaction(transaction, e, newNamesToOldNames,
								taxonomy);
						utilityPe += projectedTransaction.prefixUtility;
						transactionsPe.add(projectedTransaction);
						transaction.offset = positionE;
					} else {
						transaction.offset = low;
					}
				}
			} else {
				for (Transaction transaction : transactionsOfP) {
					// Increase the number of transaction read
					transactionReadingCount++;

					// To record the time for performing binary searh

					if (transaction.parentsInTransaction.get(e) != null) {
						Transaction projectedTransaction = new Transaction(transaction, e, taxonomy,
								newNamesToOldNames);
						utilityPe += projectedTransaction.prefixUtility;
						transactionsPe.add(projectedTransaction);
					}
				}
			}
			
			// record the time spent for performing the binary search

			// System.out.println(xx);
			// remember the total time for peforming the database projection
			timeIntersections += (System.currentTimeMillis() - timeFirstIntersection);
			// Append item "e" to P to obtain P U {e}
			// but at the same time translate from new name of "e" to its old name
			temp[prefixLength] = newNamesToOldNames[e];

			// if the utility of PU{e} is enough to be a high utility itemset

			// ==== Next, we will calculate the Local Utility and Sub-tree utility of
			// all items that could be appended to PU{e} ====
			useUtilityBinArraysToCalculateUpperBounds(transactionsPe, x, itemsToKeep);
			
			// we now record time for identifying promising items

			// We will create the new list of secondary items
			List<Integer> newItemsToKeep = new ArrayList<Integer>();
			// We will create the new list of primary items
		

			// for each item
			for (int k = x + 1; k < itemsToKeep.size(); k++) {
				Integer itemk = itemsToKeep.get(k);

				// if the sub-tree utility is no less than min util
				if (utilityBinArraySU[itemk] >= minUtil && itemk > e) {
					// and if sub-tree utility pruning is activated
					
					// consider that item as a secondary item
					newItemsToKeep.add(itemk);
				} else if (utilityBinArrayLU[itemk] >= minUtil && itemk > e) {
					// otherwise, if local utility is no less than minutil,
					// consider this itemt to be a secondary item
					newItemsToKeep.add(itemk);
				}
			}
			// update the total time for identifying promising items

			if (utilityPe >= minUtil) {
				output(prefixLength, utilityPe);

			}

			// === recursive call to explore larger itemsets
			backtrackingEFIM(transactionsPe, newItemsToKeep, newItemsToKeep, prefixLength + 1);

		}

		// check the maximum memory usage for statistics purpose
		MemoryLogger.getInstance().checkMemory();

	}

	/**
	 * Check if two transaction are identical
	 * 
	 * @param t1 the first transaction
	 * @param t2 the second transaction
	 * @return true if they are equal
	 */

	/**
	 * Scan the initial database to calculate the local utility of each item using a
	 * utility-bin array
	 * 
	 * @param dataset the transaction database
	 */
	public void useUtilityBinArrayToCalculateLocalUtilityFirstTime(Dataset dataset) {

		// Initialize utility bins for all items
		utilityBinArrayLU = new double[dataset.getMaxItem() + 1];

		HashSet<Integer> SetParent;
		// Scan the database to fill the utility bins
		// For each transaction
		for (Transaction transaction : dataset.getTransactions()) {
			SetParent = new HashSet<Integer>();
			// for each item
			for (Integer item : transaction.getItems()) {
				// we add the transaction utility to the utility bin of the item
				utilityBinArrayLU[item] += transaction.transactionUtility;
				TaxonomyNode itemIParent = taxonomy.mapItemToTaxonomyNode.get(item).getParent();
				while (itemIParent.getData() != -1) {

					int dataOfParent = itemIParent.getData();
					SetParent.add(dataOfParent);
					itemIParent = itemIParent.getParent();
				}

			}
			for (Integer item : SetParent) {
				utilityBinArrayLU[item] += transaction.transactionUtility;
			}
		}
	}

	/**
	 * Scan the initial database to calculate the sub-tree utility of each item
	 * using a utility-bin array
	 * 
	 * @param dataset the transaction database
	 */
	public void useUtilityBinArrayToCalculateSubtreeUtilityFirstTime(Dataset dataset) {

		double sumSU;
		// Scan the database to fill the utility-bins of each item
		// For each transaction
		for (Transaction transaction : dataset.getTransactions()) {
			// We will scan the transaction backward. Thus,
			// the current sub-tree utility in that transaction is zero
			// for the last item of the transaction.
			sumSU = 0;

			// For each item when reading the transaction backward
			for (int i = transaction.getItems().length - 1; i >= 0; i--) {
				// get the item
				Integer item = transaction.getItems()[i];

				// we add the utility of the current item to its sub-tree utility
				sumSU += transaction.getUtilities()[i];
				// we add the current sub-tree utility to the utility-bin of the item
				utilityBinArraySU[item] += sumSU;
			}
			for (int itemParent : transaction.parentsInTransaction.keySet()) {
				sumSU = transaction.OldTU;
				// For each item when reading the transaction backward
				for (int i = 0; i < transaction.items.length; i++) {
					// get the item
					Integer item = transaction.getItems()[i];
					if (CheckParent(newNamesToOldNames[itemParent], newNamesToOldNames[item]) == false
							&& itemParent > item) {
						sumSU -= transaction.getUtilities()[i];
					}
				}
				// sumSU -= transaction.parentsInTransaction.get(itemParent);
				utilityBinArraySU[itemParent] += sumSU;
			}
		}
	}

	/**
	 * Utilize the utility-bin arrays to calculate the sub-tree utility and local
	 * utility of all items that can extend itemset P U {e}
	 * 
	 * @param transactions the projected database for P U {e}
	 * @param j            the position of j in the list of promising items
	 * @param itemsToKeep  the list of promising items
	 */
	private void useUtilityBinArraysToCalculateUpperBounds(List<Transaction> transactionsPe, int j,
			List<Integer> itemsToKeep) {
		long initialTime = System.currentTimeMillis();

		for (int i = j + 1; i < itemsToKeep.size(); i++) {
			Integer item = itemsToKeep.get(i);
			utilityBinArraySU[item] = 0;
			utilityBinArrayLU[item] = 0;
		}
		double sumRemainingUtility;
		for (Transaction transaction : transactionsPe) {
			transactionReadingCount++;
			sumRemainingUtility = 0;

			for (int i = transaction.getItems().length - 1; i >= 0; i--) {
				// get the item
				int item = transaction.getItems()[i];
				
					// We add the utility of this item to the sum of remaining utility
					sumRemainingUtility += transaction.getUtilities()[i];
					// We update the sub-tree utility of that item in its utility-bin
					utilityBinArraySU[item] += sumRemainingUtility + transaction.prefixUtility;
					// We update the local utility of that item in its utility-bin
					utilityBinArrayLU[item] += transaction.prefixUtility+transaction.transactionUtility;

			}
			for (Integer itemParent : transaction.parentsInTransaction.keySet()) {
				double sumU = transaction.transactionUtility;
				// We add the utility of this item to the sum of remaining utility
				for (int i = 0; i < transaction.getItems().length; i++) {
					int item = transaction.getItems()[i];
					if (CheckParent(newNamesToOldNames[itemParent], newNamesToOldNames[item]) == false) {
						if (itemParent > item) {
							sumU -= transaction.getUtilities()[i];
						}
					}
				}
				utilityBinArraySU[itemParent] += transaction.prefixUtility+sumU;
				utilityBinArrayLU[itemParent] += transaction.prefixUtility+transaction.transactionUtility;

			}
		}

		// we update the time for database reduction for statistics purpose
		timeDatabaseReduction += (System.currentTimeMillis() - initialTime);
	}

	/**
	 * Save a high-utility itemset to file or memory depending on what the user
	 * chose.
	 * 
	 * @param itemset the itemset
	 * @throws IOException if error while writting to output file
	 */
	private void output(int tempPosition, Double utility) throws IOException {

		patternCount++;

//		StringBuffer buffer = new StringBuffer();
//		for (int i = 0; i <= tempPosition; i++) {
//			buffer.append(temp[i]);
//			if (i != tempPosition) {
//				buffer.append('	');
//			}
//		} // append the utility of the itemset
//		buffer.append(" #UTIL: ");
//		buffer.append(utility);
//
//		System.out.println(buffer.toString());

	}

	public static void printPeakHeapUsage() {
		try {
			List<MemoryPoolMXBean> pools = ManagementFactory.getMemoryPoolMXBeans();
			// we print the result in the console
			double total = 0;
			for (MemoryPoolMXBean memoryPoolMXBean : pools) {
				if (memoryPoolMXBean.getType() == MemoryType.HEAP) {
					long peakUsed = memoryPoolMXBean.getPeakUsage().getUsed();
					// System.out.println(String.format("Peak used for: %s is %.2f",
					// memoryPoolMXBean.getName(), (double)peakUsed/1024/1024));
					total = total + peakUsed;
				}
			}
			System.out.println(String.format("Total heap peak used: %f MB", total / 1024 / 1024));

		} catch (Throwable t) {
			System.err.println("Exception in agent: " + t);
		}
	}

	/**
	 * Print statistics about the latest execution of the EFIM algorithm.
	 */
	public void printStats() {

		System.out.println("========== EFIMNoLu v97 - STATS ============");
		System.out.println(" minUtil = " + minUtil);
		System.out.println(" High utility itemsets count: " + patternCount);
		System.out.println(" Total time ~: " + (endTimestamp - startTimestamp) + " ms");
		System.out.println(" Transaction merge count ~: " + mergeCount);
		System.out.println(" Transaction read count ~: " + transactionReadingCount + "/" + timeProject);

		// if in debug mode, we show more information
		if (DEBUG) {

			System.out.println(" Time intersections ~: " + timeIntersections + " ms");
			System.out.println(" Time database reduction ~: " + timeDatabaseReduction + " ms");
			System.out.println(" Time promising items ~: " + timeIdentifyPromisingItems + " ms");
			System.out.println(" Time binary search ~: " + timeBinarySearch + " ms");
			System.out.println(" Time sort ~: " + timeSort + " ms");
		}
		System.out.println(" Max memory:" + MemoryLogger.getInstance().getMaxMemory());
		System.out.println(" Candidate count : " + candidateCount);
		System.out.println("=====================================");
	}

	private boolean CheckParent(int item1, int item2) {
		TaxonomyNode nodeItem1 = taxonomy.getMapItemToTaxonomyNode().get(item1);
		TaxonomyNode nodeItem2 = taxonomy.getMapItemToTaxonomyNode().get(item2);
		int levelOfItem1 = nodeItem1.getLevel();
		int levelOfItem2 = nodeItem2.getLevel();
		if (levelOfItem1 == levelOfItem2) {
			return false;
		} else {
			if (levelOfItem1 > levelOfItem2) {
				TaxonomyNode parentItem1 = nodeItem1.getParent();
				while (parentItem1.getData() != -1) {
					if (parentItem1.getData() == nodeItem2.getData()) {
						return true;
					}
					parentItem1 = parentItem1.getParent();
				}
				return false;
			} else {
				TaxonomyNode parentItem2 = nodeItem2.getParent();
				while (parentItem2.getData() != -1) {
					if (parentItem2.getData() == nodeItem1.getData()) {
						return true;
					}
					parentItem2 = parentItem2.getParent();
				}
				return false;
			}
		}
	}
}
