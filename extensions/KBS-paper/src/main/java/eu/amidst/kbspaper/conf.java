package eu.amidst.kbspaper;

/**
 * Created by rcabanas on 14/08/2018.
 */
public class conf {


    static final long MILLION = 1000000L;
    static final long THOUSAND = 1000L;

    //public static String datafolder = "./datasets/amidst_hugin/";
    public static String datafolder = "~/datasets/amidst_hugin/";
    public static String netfolder = "./networks/amidst_hugin/";
    public static String logfolder = "./extensions/KBS-paper/log/";


    static String[] BNs = {"fa"};


    public static long s_init = (long) (2.5*MILLION);
    public static long s_step = (long) (2.5*MILLION);
    public static long s_stop = (long) (20*MILLION);

    public static int pca_numStatesHiddenVar = 3;
    public static int mfa_numLatent = 3;
    public static int mfa_numStatesLatent = 2;
    public static int fa_numLatent = 3;

    public static int numvars = 10;

}
