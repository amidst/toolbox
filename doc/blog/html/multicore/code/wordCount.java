package java8paper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by rcabanas on 03/10/16.
 */
public class wordCount {
	public static void main(String[] args) {


		//List of 'documents'
		List<String> docs = new ArrayList<String>();
		docs.add("we discuss software design issues related to the development of parallel");
		docs.add("computational intelligence algorithms on multi-core CPUs, using the new Java 8");
		docs.add("functional programming features. In particular, we focus on probabilistic graphical ");
		docs.add("models (PGMs) and present the parallelization of a collection of algorithms that");
		docs.add("deal with inference and learning of PGMs from data. Namely, maximum likelihood ");
		docs.add("estimation, importance sampling, and greedy search for solving combinatorial ");
		docs.add("optimization problems.Through these concrete examples, we tackle the problem of ");


		// Count the number of occurences
		Map<String, Integer> wordcount =
				docs.parallelStream()
				.flatMap(doc -> Stream.of(doc.split("\\s+")))
				.collect(
						Collectors.toMap(s -> s,			//key mapper
								s -> 1,						//value mapper
								(a, b) -> Integer.sum(a, b)	//merge function
						));

		// Prints the result
		wordcount.forEach((w,n)->System.out.println(w+"->"+n));

	}
	


}
