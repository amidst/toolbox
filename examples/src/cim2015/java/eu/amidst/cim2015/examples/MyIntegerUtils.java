package eu.amidst.cim2015.examples;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Hanen on 12/06/15.
 */
public class MyIntegerUtils {


    public interface NbrPredicate{
        boolean test(Integer integer);
    }

    public static boolean isEven(Integer i){
        return i%2==0;
    }

    public static boolean isOdd(Integer i){
        return i%2==1;
    }

    public static int filterList(List<Integer> input, NbrPredicate predicate){
        int count = 0;
        for (Integer i : input){
            if (predicate.test(i))
               count++;
        }
        return count;
    }

    public static void main(String[] args) throws Exception {

        NbrPredicate preEven = new NbrPredicate() {
            @Override
            public boolean test(Integer integer) {
                return isEven(integer);
            }
        };

        NbrPredicate preOdd = new NbrPredicate() {
            @Override
            public boolean test(Integer integer) {
                return isOdd(integer);
            }
        };



        List<Integer> list= Arrays.asList(0, 3, 5, 6, 8, 10, 12, 15);

        System.out.println("The count of Even numbers is " + MyIntegerUtils.filterList(list, preEven));
        System.out.println("The count of Odd numbers is " +  MyIntegerUtils.filterList(list, preOdd));

        System.out.println("The count of Even numbers is " + MyIntegerUtils.filterList(list, MyIntegerUtils::isEven));
        System.out.println("The count of Odd numbers is " +  MyIntegerUtils.filterList(list, MyIntegerUtils::isOdd));
    }
}
