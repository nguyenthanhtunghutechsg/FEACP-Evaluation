<h1 ><strong>FEACP Evaluation</strong></h1>
<p>Project FEACP-Evaluation was created to test the performance of FEACP, FEACP(without sub-tree utility), and CLH-Miner algorithms. The project includes the source code and information about the dataset and the taxonomy corresponding to that dataset.</p>
<h2 >Prerequisites</h2>
<p> Before you continue, ensure you meet the following requirements: </p>
<ul>
<li>You have installed the Eclipse and JDK 8 or later. </li>
<li>You are using a Windows machine. </li>

</ul>
<h2 >Structure of Project</h2>
<ul>
<li><p>The source code of the project is located in the &quot;src&quot; folder with the following details:</p>
<ul>
<li>FEACP: /tree/master/src/FEACP</li>
<li>FEACP:/tree/master/src/FEACPLu</li>
<li>CLH-Miner: tree/master/src/CLHMiner</li>

</ul>
</li>
<li><p>The dataset and Taxonomoy  is located in the &quot;Dataset&quot; folder with the following details</p>
<ul>
<li><p>Each dataset includes 1 dataset file containing transaction information and 1 taxonomy file containing hierarchical information</p>
<ul>
<li>File dataset: named as <name dataset>.txt contains transaction information. Each transaction consists of 3 parts, separated by a &quot;:&quot; character. Part 1 includes the item id, part 2 is transaction utility and the rest will contain the utility of each item in the transaction.</li>
<li>File Taxonomy: named as &quot;namedataset&quot;Taxonomy.txt contains information about the relationships of the items. relationships represented by child-parent pairs.</li>

</ul>
</li>

</ul>
</li>

</ul>
<h2 >Install</h2>
<p>	Unzip and import the project into eclipse.</p>
<h2 >Usage</h2>
<p>	Select the corresponding dataset and taxonomy Then select a minU threshold and click Run</p>
<h2 >Author</h2>
<ul>
<li>Bay Vo et al.</li>

</ul>
