package java8paper;

import eu.amidst.core.models.DAG;
import eu.amidst.core.models.ParentSet;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Created by rcabanas on 03/10/16.
 */
public class NumberEvenOdd {


	public static void main(String[] args) {
		List<Integer> list = Arrays.asList(1,3,5,2,45,10,3,34);

		//Count the number of even numbers
		long countEven = list.parallelStream()
				.filter(i -> i % 2 == 0)
				.count();

		System.out.println("number of even numbers is "+countEven);


	}






}
