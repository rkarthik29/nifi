/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nifi.processors.standard;

import static org.junit.Assert.assertEquals;
import org.apache.nifi.flowfile.attributes.CoreAttributes;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class TestSplitText {

    final String originalFilename = "original.txt";
    final Path dataPath = Paths.get("src/test/resources/TestSplitText");
    final Path file = dataPath.resolve(originalFilename);
    final static String TEST_INPUT_DATA = "HeaderLine1\nLine2SpacesAtEnd  \nLine3\nLine4\n\n\nLine8\nLine9\n\n\n13\n14\n15    EndofLine15\n16\n"
            + "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\nLastLine\n";
    final static String TEST_SPLIT_DATA="test data \n has newline\r\nmay have it \n more than once\r\n";
    @Test
    public void testRoutesToFailureIfHeaderLinesNotAllPresent() throws IOException {
        final TestRunner runner = TestRunners.newTestRunner(new SplitText());
        runner.setProperty(SplitText.HEADER_LINE_COUNT, "100");
        runner.setProperty(SplitText.LINE_SPLIT_COUNT, "3");

        runner.enqueue(file);
        runner.run();
        runner.assertAllFlowFilesTransferred(SplitText.REL_FAILURE, 1);
    }

    @Test
    public void testZeroByteOutput() throws IOException {
        final TestRunner runner = TestRunners.newTestRunner(new SplitText());
        runner.setProperty(SplitText.HEADER_LINE_COUNT, "0");
        runner.setProperty(SplitText.LINE_SPLIT_COUNT, "3");

        runner.enqueue(file);
        runner.run();
        runner.assertTransferCount(SplitText.REL_SPLITS, 4);
    }

    @Test
    public void testSplitWithoutHeader() throws IOException {
        final TestRunner runner = TestRunners.newTestRunner(new SplitText());
        runner.setProperty(SplitText.HEADER_LINE_COUNT, "0");
        runner.setProperty(SplitText.LINE_SPLIT_COUNT, "3");
        runner.setProperty(SplitText.LINE_END_CHAR, "LF");
        runner.enqueue(file);
        runner.run();

        runner.assertTransferCount(SplitText.REL_FAILURE, 0);
        runner.assertTransferCount(SplitText.REL_ORIGINAL, 1);
        runner.assertTransferCount(SplitText.REL_SPLITS, 4);

        final List<MockFlowFile> splits = runner.getFlowFilesForRelationship(SplitText.REL_SPLITS);

        final String expected0 = "Header Line #1\nHeader Line #2\nLine #1";
        final String expected1 = "Line #2\nLine #3\nLine #4";
        final String expected2 = "Line #5\nLine #6\nLine #7";
        final String expected3 = "Line #8\nLine #9\nLine #10";

        splits.get(0).assertContentEquals(expected0);
        splits.get(1).assertContentEquals(expected1);
        splits.get(2).assertContentEquals(expected2);
        splits.get(3).assertContentEquals(expected3);
    }
    
    @Test
    public void testNewLineinSplitData() throws IOException {
        final TestRunner runner = TestRunners.newTestRunner(new SplitText());
        runner.setProperty(SplitText.HEADER_LINE_COUNT, "0");
        runner.setProperty(SplitText.LINE_SPLIT_COUNT, "1");
        runner.setProperty(SplitText.LINE_END_CHAR, "CRLF");

        runner.enqueue(TEST_SPLIT_DATA);
        runner.run();

        runner.assertTransferCount(SplitText.REL_FAILURE, 0);
        runner.assertTransferCount(SplitText.REL_ORIGINAL, 1);
        runner.assertTransferCount(SplitText.REL_SPLITS, 2);

        final List<MockFlowFile> splits = runner.getFlowFilesForRelationship(SplitText.REL_SPLITS);
        splits.get(0).assertContentEquals("test data \n has newline");
        splits.get(1).assertContentEquals("may have it \n more than once");

    }
    
    @Test
    public void testNewLineinHeaderAndSplitData() throws IOException {
        final TestRunner runner = TestRunners.newTestRunner(new SplitText());
        runner.setProperty(SplitText.HEADER_LINE_COUNT, "0");
        runner.setProperty(SplitText.LINE_SPLIT_COUNT, "1");
        runner.setProperty(SplitText.LINE_END_CHAR, "CRLF");
        
        runner.enqueue("New lines may\r be in header\r\ntest data \n has newline\r\nmay have it \n more than once\r\n");
        runner.run();

        runner.assertTransferCount(SplitText.REL_FAILURE, 0);
        runner.assertTransferCount(SplitText.REL_ORIGINAL, 1);
        runner.assertTransferCount(SplitText.REL_SPLITS, 3);

        final List<MockFlowFile> splits = runner.getFlowFilesForRelationship(SplitText.REL_SPLITS);
        splits.get(0).assertContentEquals("New lines may\r be in header");
        splits.get(1).assertContentEquals("test data \n has newline");
        splits.get(2).assertContentEquals("may have it \n more than once");

    }

    @Test
    public void testOneLineSplitWithoutHeader() throws IOException {
        final TestRunner runner = TestRunners.newTestRunner(new SplitText());
        runner.setProperty(SplitText.HEADER_LINE_COUNT, "0");
        runner.setProperty(SplitText.LINE_SPLIT_COUNT, "1");

        runner.enqueue(TEST_INPUT_DATA);
        runner.run();

        runner.assertTransferCount(SplitText.REL_FAILURE, 0);
        runner.assertTransferCount(SplitText.REL_ORIGINAL, 1);
        runner.assertTransferCount(SplitText.REL_SPLITS, 11);

        final List<MockFlowFile> splits = runner.getFlowFilesForRelationship(SplitText.REL_SPLITS);

        splits.get(0).assertContentEquals("HeaderLine1");
        splits.get(1).assertContentEquals("Line2SpacesAtEnd  ");
        splits.get(2).assertContentEquals("Line3");
        splits.get(3).assertContentEquals("Line4");
        splits.get(4).assertContentEquals("Line8");
        splits.get(5).assertContentEquals("Line9");
        splits.get(6).assertContentEquals("13");
        splits.get(7).assertContentEquals("14");
        splits.get(8).assertContentEquals("15    EndofLine15");
        splits.get(9).assertContentEquals("16");
        splits.get(10).assertContentEquals("LastLine");
    }

    @Test
    public void testFiveLineSplitWithoutHeader() throws IOException {
        final TestRunner runner = TestRunners.newTestRunner(new SplitText());
        runner.setProperty(SplitText.HEADER_LINE_COUNT, "0");
        runner.setProperty(SplitText.LINE_SPLIT_COUNT, "5");

        runner.enqueue(TEST_INPUT_DATA);
        runner.run();

        runner.assertTransferCount(SplitText.REL_FAILURE, 0);
        runner.assertTransferCount(SplitText.REL_ORIGINAL, 1);
        runner.assertTransferCount(SplitText.REL_SPLITS, 4);

        final List<MockFlowFile> splits = runner.getFlowFilesForRelationship(SplitText.REL_SPLITS);

        splits.get(0).assertContentEquals("HeaderLine1\nLine2SpacesAtEnd  \nLine3\nLine4");
        splits.get(1).assertContentEquals("\nLine8\nLine9");
        splits.get(2).assertContentEquals("13\n14\n15    EndofLine15\n16");
        splits.get(3).assertContentEquals("\n\nLastLine");
    }

    @Test
    public void testFiveLineSplitWithoutHeaderRetainNewline() throws IOException {
        final TestRunner runner = TestRunners.newTestRunner(new SplitText());
        runner.setProperty(SplitText.HEADER_LINE_COUNT, "0");
        runner.setProperty(SplitText.LINE_SPLIT_COUNT, "5");
        runner.setProperty(SplitText.REMOVE_TRAILING_NEWLINES, "false");

        runner.enqueue(TEST_INPUT_DATA);
        runner.run();

        runner.assertTransferCount(SplitText.REL_FAILURE, 0);
        runner.assertTransferCount(SplitText.REL_ORIGINAL, 1);
        runner.assertTransferCount(SplitText.REL_SPLITS, 10);

        final List<MockFlowFile> splits = runner.getFlowFilesForRelationship(SplitText.REL_SPLITS);

        splits.get(0).assertContentEquals("HeaderLine1\nLine2SpacesAtEnd  \nLine3\nLine4\n\n");
        splits.get(1).assertContentEquals("\nLine8\nLine9\n\n\n");
        splits.get(2).assertContentEquals("13\n14\n15    EndofLine15\n16\n\n");
        splits.get(3).assertContentEquals("\n\n\n\n\n");
        splits.get(4).assertContentEquals("\n\n\n\n\n");
        splits.get(5).assertContentEquals("\n\n\n\n\n");
        splits.get(6).assertContentEquals("\n\n\n\n\n");
        splits.get(7).assertContentEquals("\n\n\n\n\n");
        splits.get(8).assertContentEquals("\n\n\n\n\n");
        splits.get(9).assertContentEquals("\n\nLastLine\n");
    }

    @Test
    public void testFiveLineSplitWithHeaderRetainNewline() throws IOException {
        final TestRunner runner = TestRunners.newTestRunner(new SplitText());
        runner.setProperty(SplitText.HEADER_LINE_COUNT, "1");
        runner.setProperty(SplitText.LINE_SPLIT_COUNT, "5");
        runner.setProperty(SplitText.REMOVE_TRAILING_NEWLINES, "false");
        runner.enqueue(TEST_INPUT_DATA);
        runner.run();

        runner.assertTransferCount(SplitText.REL_FAILURE, 0);
        runner.assertTransferCount(SplitText.REL_ORIGINAL, 1);
        runner.assertTransferCount(SplitText.REL_SPLITS, 10);

        final List<MockFlowFile> splits = runner.getFlowFilesForRelationship(SplitText.REL_SPLITS);

        splits.get(0).assertContentEquals("HeaderLine1\nLine2SpacesAtEnd  \nLine3\nLine4\n\n\n");
        splits.get(1).assertContentEquals("HeaderLine1\nLine8\nLine9\n\n\n13\n");
        splits.get(2).assertContentEquals("HeaderLine1\n14\n15    EndofLine15\n16\n\n\n");
        splits.get(3).assertContentEquals("HeaderLine1\n\n\n\n\n\n");
        splits.get(4).assertContentEquals("HeaderLine1\n\n\n\n\n\n");
        splits.get(5).assertContentEquals("HeaderLine1\n\n\n\n\n\n");
        splits.get(6).assertContentEquals("HeaderLine1\n\n\n\n\n\n");
        splits.get(7).assertContentEquals("HeaderLine1\n\n\n\n\n\n");
        splits.get(8).assertContentEquals("HeaderLine1\n\n\n\n\n\n");
        splits.get(9).assertContentEquals("HeaderLine1\n\nLastLine\n");
    }

    @Test
    public void testFiveLineSplitWithHeaderNotRetainNewline() throws IOException {
        final TestRunner runner = TestRunners.newTestRunner(new SplitText());
        runner.setProperty(SplitText.HEADER_LINE_COUNT, "1");
        runner.setProperty(SplitText.LINE_SPLIT_COUNT, "5");
        runner.setProperty(SplitText.REMOVE_TRAILING_NEWLINES, "true");

        runner.enqueue(TEST_INPUT_DATA);
        runner.run();

        runner.assertTransferCount(SplitText.REL_FAILURE, 0);
        runner.assertTransferCount(SplitText.REL_ORIGINAL, 1);
        runner.assertTransferCount(SplitText.REL_SPLITS, 10);

        final List<MockFlowFile> splits = runner.getFlowFilesForRelationship(SplitText.REL_SPLITS);
        splits.get(0).assertContentEquals("HeaderLine1\nLine2SpacesAtEnd  \nLine3\nLine4");
        splits.get(1).assertContentEquals("HeaderLine1\nLine8\nLine9\n\n\n13");
        splits.get(2).assertContentEquals("HeaderLine1\n14\n15    EndofLine15\n16");
        splits.get(3).assertContentEquals("HeaderLine1");
        splits.get(4).assertContentEquals("HeaderLine1");
        splits.get(5).assertContentEquals("HeaderLine1");
        splits.get(6).assertContentEquals("HeaderLine1");
        splits.get(7).assertContentEquals("HeaderLine1");
        splits.get(8).assertContentEquals("HeaderLine1");
        splits.get(9).assertContentEquals("HeaderLine1\n\nLastLine");
    }

    @Test
    public void testOneLineSplitWithHeader() throws IOException {
        final TestRunner runner = TestRunners.newTestRunner(new SplitText());
        runner.setProperty(SplitText.HEADER_LINE_COUNT, "1");
        runner.setProperty(SplitText.LINE_SPLIT_COUNT, "1");

        runner.enqueue(TEST_INPUT_DATA);
        runner.run();

        runner.assertTransferCount(SplitText.REL_FAILURE, 0);
        runner.assertTransferCount(SplitText.REL_ORIGINAL, 1);
        runner.assertTransferCount(SplitText.REL_SPLITS, 47);

        final List<MockFlowFile> splits = runner.getFlowFilesForRelationship(SplitText.REL_SPLITS);

        splits.get(0).assertContentEquals("HeaderLine1\nLine2SpacesAtEnd  ");
        splits.get(1).assertContentEquals("HeaderLine1\nLine3");
        splits.get(2).assertContentEquals("HeaderLine1\nLine4");
        splits.get(3).assertContentEquals("HeaderLine1");
        splits.get(4).assertContentEquals("HeaderLine1");
        splits.get(5).assertContentEquals("HeaderLine1\nLine8");
        splits.get(6).assertContentEquals("HeaderLine1\nLine9");
        splits.get(7).assertContentEquals("HeaderLine1");
        splits.get(8).assertContentEquals("HeaderLine1");
        splits.get(9).assertContentEquals("HeaderLine1\n13");
        splits.get(10).assertContentEquals("HeaderLine1\n14");
        splits.get(11).assertContentEquals("HeaderLine1\n15    EndofLine15");
        splits.get(12).assertContentEquals("HeaderLine1\n16");
        for (int i = 13; i < 46; i++) {
            splits.get(i).assertContentEquals("HeaderLine1");
        }
        splits.get(46).assertContentEquals("HeaderLine1\nLastLine");
    }

    @Test
    public void testSplitWithTwoLineHeader() throws IOException {
        final TestRunner runner = TestRunners.newTestRunner(new SplitText());
        runner.setProperty(SplitText.HEADER_LINE_COUNT, "2");
        runner.setProperty(SplitText.LINE_SPLIT_COUNT, "3");

        runner.enqueue(file);
        runner.run();

        runner.assertTransferCount(SplitText.REL_FAILURE, 0);
        runner.assertTransferCount(SplitText.REL_ORIGINAL, 1);
        runner.assertTransferCount(SplitText.REL_SPLITS, 4);

        final List<MockFlowFile> splits = runner.getFlowFilesForRelationship(SplitText.REL_SPLITS);
        for (int i = 0; i < splits.size(); i++) {
            final MockFlowFile split = splits.get(i);
            split.assertContentEquals(file.getParent().resolve((i + 1) + ".txt"));
            split.assertAttributeEquals(SplitText.FRAGMENT_INDEX, String.valueOf(i + 1));
        }
    }

    @Test
    public void testSplitWithTwoLineHeaderAndEvenMultipleOfLines() throws IOException {
        final TestRunner runner = TestRunners.newTestRunner(new SplitText());
        runner.setProperty(SplitText.HEADER_LINE_COUNT, "2");
        runner.setProperty(SplitText.LINE_SPLIT_COUNT, "5");

        runner.enqueue(file);
        runner.run();

        runner.assertTransferCount(SplitText.REL_FAILURE, 0);
        runner.assertTransferCount(SplitText.REL_ORIGINAL, 1);
        runner.assertTransferCount(SplitText.REL_SPLITS, 2);

        final List<MockFlowFile> splits = runner.getFlowFilesForRelationship(SplitText.REL_SPLITS);
        splits.get(0).assertContentEquals(file.getParent().resolve("5.txt"));
        splits.get(0).assertAttributeEquals(SplitText.FRAGMENT_INDEX, String.valueOf(1));
        splits.get(1).assertContentEquals(file.getParent().resolve("6.txt"));
        splits.get(1).assertAttributeEquals(SplitText.FRAGMENT_INDEX, String.valueOf(2));
    }

    @Test
    public void testSplitThenMerge() throws IOException {
        final TestRunner splitRunner = TestRunners.newTestRunner(new SplitText());
        splitRunner.setProperty(SplitText.LINE_SPLIT_COUNT, "3");
        splitRunner.setProperty(SplitText.REMOVE_TRAILING_NEWLINES, "false");

        splitRunner.enqueue(file);
        splitRunner.run();

        splitRunner.assertTransferCount(SplitText.REL_SPLITS, 4);
        splitRunner.assertTransferCount(SplitText.REL_ORIGINAL, 1);
        splitRunner.assertTransferCount(SplitText.REL_FAILURE, 0);

        final List<MockFlowFile> splits = splitRunner.getFlowFilesForRelationship(SplitText.REL_SPLITS);
        for (final MockFlowFile flowFile : splits) {
            flowFile.assertAttributeEquals(SplitText.SEGMENT_ORIGINAL_FILENAME, originalFilename);
            flowFile.assertAttributeEquals(SplitText.FRAGMENT_COUNT, "4");
        }

        final TestRunner mergeRunner = TestRunners.newTestRunner(new MergeContent());
        mergeRunner.setProperty(MergeContent.MERGE_FORMAT, MergeContent.MERGE_FORMAT_CONCAT);
        mergeRunner.setProperty(MergeContent.MERGE_STRATEGY, MergeContent.MERGE_STRATEGY_DEFRAGMENT);
        mergeRunner.enqueue(splits.toArray(new MockFlowFile[0]));
        mergeRunner.run();

        mergeRunner.assertTransferCount(MergeContent.REL_MERGED, 1);
        mergeRunner.assertTransferCount(MergeContent.REL_ORIGINAL, 4);
        mergeRunner.assertTransferCount(MergeContent.REL_FAILURE, 0);

        final List<MockFlowFile> packed = mergeRunner.getFlowFilesForRelationship(MergeContent.REL_MERGED);
        MockFlowFile flowFile = packed.get(0);
        flowFile.assertAttributeEquals(CoreAttributes.FILENAME.key(), originalFilename);
        assertEquals(Files.size(dataPath.resolve(originalFilename)), flowFile.getSize());
        flowFile.assertContentEquals(file);
    }


    /*
     * If an input FlowFile has a number of blank lines greater than the Line Split Count property,
     * ensure that the remainder of the FlowFile will be processed, resulting in no data loss.
     */
    @Test
    public void testSplitWithOnlyNewLines() {
        final TestRunner splitRunner = TestRunners.newTestRunner(new SplitText());
        splitRunner.setProperty(SplitText.HEADER_LINE_COUNT, "2");
        splitRunner.setProperty(SplitText.LINE_SPLIT_COUNT, "3");
        splitRunner.setProperty(SplitText.REMOVE_TRAILING_NEWLINES, "true");
        splitRunner.setProperty(SplitText.LINE_END_CHAR, "LF");

        splitRunner.enqueue("H1\nH2\n1\n2\n3\n\n\n\n\n\n\n10\n11\n12\n");

        splitRunner.run();
        splitRunner.assertTransferCount(SplitText.REL_SPLITS, 4);
        splitRunner.assertTransferCount(SplitText.REL_ORIGINAL, 1);
        splitRunner.assertTransferCount(SplitText.REL_FAILURE, 0);

        final List<MockFlowFile> splits = splitRunner.getFlowFilesForRelationship(SplitText.REL_SPLITS);
        splits.get(0).assertContentEquals("H1\nH2\n1\n2\n3");
        splits.get(1).assertContentEquals("H1\nH2");
        splits.get(2).assertContentEquals("H1\nH2");
        splits.get(3).assertContentEquals("H1\nH2\n10\n11\n12");

        splitRunner.clearTransferState();
        splitRunner.setProperty(SplitText.HEADER_LINE_COUNT, "0");
        splitRunner.setProperty(SplitText.LINE_SPLIT_COUNT, "3");
        splitRunner.setProperty(SplitText.REMOVE_TRAILING_NEWLINES, "true");
        splitRunner.setProperty(SplitText.LINE_END_CHAR, "LF");

        splitRunner.enqueue("1\n2\n3\n\n\n\n\n\n\n10\n11\n12\n");

        splitRunner.run();
        splitRunner.assertTransferCount(SplitText.REL_SPLITS, 2);
        splitRunner.assertTransferCount(SplitText.REL_ORIGINAL, 1);
        splitRunner.assertTransferCount(SplitText.REL_FAILURE, 0);

        final List<MockFlowFile> splitsWithNoHeader = splitRunner.getFlowFilesForRelationship(SplitText.REL_SPLITS);
        splitsWithNoHeader.get(0).assertContentEquals("1\n2\n3");
        splitsWithNoHeader.get(1).assertContentEquals("10\n11\n12");

    }

    /*
     * If an input FlowFile has X blank lines at the end of a file and Line Split Count is
     * greater than X, verify that newlines are removed.
     */
    @Test
    public void testWithLotsOfBlankLinesAtEnd() {
        // verify with header lines
        final TestRunner splitRunner = TestRunners.newTestRunner(new SplitText());
        splitRunner.setProperty(SplitText.HEADER_LINE_COUNT, "2");
        splitRunner.setProperty(SplitText.LINE_SPLIT_COUNT, "10");
        splitRunner.setProperty(SplitText.REMOVE_TRAILING_NEWLINES, "true");

        splitRunner.enqueue("H1\nH2\n1\n\n\n");

        splitRunner.run();
        splitRunner.assertTransferCount(SplitText.REL_SPLITS, 1);
        splitRunner.assertTransferCount(SplitText.REL_ORIGINAL, 1);
        splitRunner.assertTransferCount(SplitText.REL_FAILURE, 0);

        final List<MockFlowFile> splits = splitRunner.getFlowFilesForRelationship(SplitText.REL_SPLITS);
        splits.get(0).assertContentEquals("H1\nH2\n1");

        // verify without headers
        splitRunner.clearTransferState();
        splitRunner.setProperty(SplitText.HEADER_LINE_COUNT, "0");

        splitRunner.enqueue("1\n2\n\n\n\n");
        splitRunner.run();

        splitRunner.assertTransferCount(SplitText.REL_SPLITS, 1);
        splitRunner.assertTransferCount(SplitText.REL_ORIGINAL, 1);
        splitRunner.assertTransferCount(SplitText.REL_FAILURE, 0);

        final List<MockFlowFile> splitsWithNoHeader = splitRunner.getFlowFilesForRelationship(SplitText.REL_SPLITS);
        splitsWithNoHeader.get(0).assertContentEquals("1\n2");
    }

    /*
     * If an input FlowFile has X blank lines at the end of a file and Header Line Count = 0,
     * ensure all newlines removed from end of file. Previous behavior was: In the case where X is greater than
     * Line Split Count, there will be split files consisting of nothing but blank lines,
     * specifically one fewer lines than Line Split Count (i.e. only the final newline character is removed).
     *
     * Ensure that the above behavior is no longer reflected by the Processor.
     */
    @Test
    public void testAllNewLinesTrimmed() {
        final TestRunner splitRunner = TestRunners.newTestRunner(new SplitText());
        splitRunner.setProperty(SplitText.HEADER_LINE_COUNT, "0");
        splitRunner.setProperty(SplitText.LINE_SPLIT_COUNT, "3");
        splitRunner.setProperty(SplitText.REMOVE_TRAILING_NEWLINES, "true");

        splitRunner.enqueue("1\n2\n\n\n\n\n\n\n\n");

        splitRunner.run();
        splitRunner.assertTransferCount(SplitText.REL_SPLITS, 1);
        splitRunner.assertTransferCount(SplitText.REL_ORIGINAL, 1);
        splitRunner.assertTransferCount(SplitText.REL_FAILURE, 0);

        final List<MockFlowFile> splits = splitRunner.getFlowFilesForRelationship(SplitText.REL_SPLITS);
        splits.get(0).assertContentEquals("1\n2");
    }


    /*
     * Previous behavior that was exhibited:
     * If an input FlowFile has X blank lines at the end of a file and
     * Header Line Count = 1 (or any non-zero value), the blank lines
     * are removed and no split file of just blanks is created. However,
     * the final line does contain a newline character. In other split
     * files, the final line has the newline character removed.
     *
     * Ensure that this behavior has been addressed. The Split file that
     * does contain content should not have the trailing new line. The
     * last FlowFile should be generated, containing nothing.
     */
    @Test
    public void testConsistentTrailingOfNewLines() {
        final TestRunner splitRunner = TestRunners.newTestRunner(new SplitText());
        splitRunner.setProperty(SplitText.HEADER_LINE_COUNT, "1");
        splitRunner.setProperty(SplitText.LINE_SPLIT_COUNT, "3");
        splitRunner.setProperty(SplitText.REMOVE_TRAILING_NEWLINES, "true");

        splitRunner.enqueue("H1\n1\n\n\n\n\n\n\n\n");

        splitRunner.run();
        splitRunner.assertTransferCount(SplitText.REL_SPLITS, 3);
        splitRunner.assertTransferCount(SplitText.REL_ORIGINAL, 1);
        splitRunner.assertTransferCount(SplitText.REL_FAILURE, 0);

        final List<MockFlowFile> splits = splitRunner.getFlowFilesForRelationship(SplitText.REL_SPLITS);
        splits.get(0).assertContentEquals("H1\n1");
        splits.get(1).assertContentEquals("H1");
        splits.get(2).assertContentEquals("H1");
    }

    @Test
    public void testWithSplitThatStartsWithNewLine() {
        final TestRunner splitRunner = TestRunners.newTestRunner(new SplitText());
        splitRunner.setProperty(SplitText.HEADER_LINE_COUNT, "1");
        splitRunner.setProperty(SplitText.LINE_SPLIT_COUNT, "3");
        splitRunner.setProperty(SplitText.REMOVE_TRAILING_NEWLINES, "true");

        splitRunner.enqueue("H1\n1\n2\n3\n\n\n4\n");

        splitRunner.run();
        splitRunner.assertTransferCount(SplitText.REL_SPLITS, 2);
        splitRunner.assertTransferCount(SplitText.REL_ORIGINAL, 1);
        splitRunner.assertTransferCount(SplitText.REL_FAILURE, 0);

        final List<MockFlowFile> splits = splitRunner.getFlowFilesForRelationship(SplitText.REL_SPLITS);
        splits.get(0).assertContentEquals("H1\n1\n2\n3");
        splits.get(1).assertContentEquals("H1\n\n\n4");

        splitRunner.clearTransferState();

        splitRunner.setProperty(SplitText.HEADER_LINE_COUNT, "0");
        splitRunner.enqueue("1\n2\n3\n\n\n4\n");

        splitRunner.run();
        splitRunner.assertTransferCount(SplitText.REL_SPLITS, 2);
        splitRunner.assertTransferCount(SplitText.REL_ORIGINAL, 1);
        splitRunner.assertTransferCount(SplitText.REL_FAILURE, 0);

        final List<MockFlowFile> splitsWithoutHeader = splitRunner.getFlowFilesForRelationship(SplitText.REL_SPLITS);
        splitsWithoutHeader.get(0).assertContentEquals("1\n2\n3");
        splitsWithoutHeader.get(1).assertContentEquals("\n\n4");
    }

    @Test
    public void testWithEmptyHeaderLines() {
        final TestRunner splitRunner = TestRunners.newTestRunner(new SplitText());
        splitRunner.setProperty(SplitText.HEADER_LINE_COUNT, "2");
        splitRunner.setProperty(SplitText.LINE_SPLIT_COUNT, "3");
        splitRunner.setProperty(SplitText.REMOVE_TRAILING_NEWLINES, "true");

        splitRunner.enqueue("\n\n1\n\n\n\n\n");

        splitRunner.run();
        splitRunner.assertTransferCount(SplitText.REL_SPLITS, 2);
        splitRunner.assertTransferCount(SplitText.REL_ORIGINAL, 1);
        splitRunner.assertTransferCount(SplitText.REL_FAILURE, 0);

        final List<MockFlowFile> splits = splitRunner.getFlowFilesForRelationship(SplitText.REL_SPLITS);
        splits.get(0).assertContentEquals("\n\n1");
        splits.get(1).assertContentEquals("");
    }

}