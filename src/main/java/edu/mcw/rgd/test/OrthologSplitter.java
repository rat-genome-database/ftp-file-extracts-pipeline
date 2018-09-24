package edu.mcw.rgd.test;

import edu.mcw.rgd.OrthologExtractor;

/**
 * Created by mtutaj on 6/16/2016.
 */
public class OrthologSplitter {

    public static void main(String[] args) throws Exception {

        OrthologExtractor extractor = new OrthologExtractor();
        extractor.splitOrthologFilesForRatmine();
    }
}
