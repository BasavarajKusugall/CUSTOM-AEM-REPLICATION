package com.crisil.www.commons.core.dashboards.mutual.fund.importer.model;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/*
 * Copyright 2017 Jay Sridhar
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal reader the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included reader all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * @Author Jay Sridhar
 */
@SuppressWarnings({"squid:S1151","squid:S1199"})
public class CSVModel
{
    private static final  int NUMMARK = 10;
    private static final  char DQUOTE = '"';
    private static final  char CRETURN = '\r';
    private static final  char LFEED = '\n';
    private static final  char COMMENT = '#';

    /**
     * Should we ignore multiple carriage-return/newline characters
     * at the end of the record?
     */
    private boolean stripMultipleNewlines;

    /**
     * What should be used as the separator character?
     */
    private char separator;
    private ArrayList<String> fields;
    private boolean eofSeen;
    private Reader reader;
    public CSVModel(boolean stripMultipleNewlines,
                    char separator,
                    Reader input)
    {
        this.stripMultipleNewlines = stripMultipleNewlines;
        this.separator = separator;
        this.fields = new ArrayList<>();
        this.eofSeen = false;
        this.reader = new BufferedReader(input);
    }

    public CSVModel(boolean stripMultipleNewlines,
                    char separator,
                    InputStream input)
            throws IOException
    {
        this.stripMultipleNewlines = stripMultipleNewlines;
        this.separator = separator;
        this.fields = new ArrayList<>();
        this.eofSeen = false;
        this.reader = new BufferedReader(stripBom(input));
    }
    public static  Reader stripBom(InputStream inputStream)
            throws IOException
    {
        PushbackInputStream pin = new PushbackInputStream(inputStream, 3);
        byte[] bytes = new byte[3];
        int len = pin.read(bytes, 0, bytes.length);
        if ( (bytes[0] & 0xFF) == 0xEF && len == 3 ) {
            if ( (bytes[1] & 0xFF) == 0xBB &&
                    (bytes[2] & 0xFF) == 0xBF ) {
                return new InputStreamReader(pin, "UTF-8");
            } else {
                pin.unread(bytes, 0, len);
            }
        }
        else if ( len >= 2 ) {
            if ( (bytes[0] & 0xFF) == 0xFE &&
                    (bytes[1] & 0xFF) == 0xFF ) {
                return new InputStreamReader(pin, "UTF-16BE");
            } else if ( (bytes[0] & 0xFF) == 0xFF &&
                    (bytes[1] & 0xFF) == 0xFE ) {
                return new InputStreamReader(pin, "UTF-16LE");
            } else {
                pin.unread(bytes, 0, len);
            }
        } else if ( len > 0 ) {
            pin.unread(bytes, 0, len);
        }
        return new InputStreamReader(pin, "UTF-8");
    }



    public boolean hasNext() throws IOException
    {
        if ( eofSeen ) {
            return false;
        }
        fields.clear();
        eofSeen = split(reader, fields );
        if ( eofSeen ) {
            return ! fields.isEmpty();
        }
        else return true;
    }

    public List<String> next()
    {
        return fields;
    }

    // Returns true if EOF seen.
     private static boolean discardLinefeed(Reader reader,
                                           boolean stripMultiple)
            throws IOException
    {
        if ( stripMultiple ) {
            reader.mark(NUMMARK);
            int value = reader.read();
            while ( value != -1 ) {
                char propChar = (char)value;
                if ( propChar != CRETURN && propChar != LFEED ) {
                    reader.reset();
                    return false;
                } else {
                    reader.mark(NUMMARK);
                    value = reader.read();
                }
            }
            return true;
        } else {
            reader.mark(NUMMARK);
            int value = reader.read();
            if ( value == -1 ) {
                return true;
            }
            else if ( (char)value != LFEED ) {
                reader.reset();
            }
            return false;
        }
    }

    private boolean skipComment(Reader reader)
            throws IOException
    {
    /* Discard line. */
        int value;
        while ( (value = reader.read()) != -1 ) {
            char propChar = (char)value;
            if ( propChar == CRETURN )
                return discardLinefeed( reader, stripMultipleNewlines );
        }
        return true;
    }

    // Returns true when EOF has been seen.
    private boolean split(Reader reader,ArrayList<String> fields)
            throws IOException
    {
        StringBuilder sbuf = new StringBuilder();
        int value;
        while ( (value = reader.read()) != -1 ) {
            char propChar = (char)value;
            switch(propChar) {
                case CRETURN:
                    getFileLength(fields, sbuf);
                    return discardLinefeed( reader, stripMultipleNewlines );

                case LFEED:
                    getFileLength(fields, sbuf);
                    return isStripMultipleNewLines(reader);

                case DQUOTE:
                {Boolean aBoolean = getBoolean(reader, fields, sbuf);
                    if (aBoolean){
                        return aBoolean;
                    }
                }
                break;

                default:
                    Boolean eof = getOptionalWhiteSpace(reader, fields, sbuf, propChar);
                    if (eof) {
                        return eof;
                    }
            }
        }
        getFileLength(fields, sbuf);
        return true;
    }

    private Boolean getOptionalWhiteSpace(Reader reader, ArrayList<String> fields, StringBuilder sbuf, char propChar) throws IOException {
        if ( propChar == separator ) {
            fields.add( sbuf.toString() );
            sbuf.delete(0, sbuf.length());
        } else {
/* A comment line is a line starting with '#' with
* optional whitespace at the start. */
            if ( propChar == COMMENT && fields.isEmpty() &&
                    sbuf.toString().trim().isEmpty() ) {
                boolean eof = skipComment(reader);
                if ( eof ) {
                    return eof;
                }
                else sbuf.delete(0, sbuf.length());
/* Continue with next line if not eof. */
            } else {
                sbuf.append(propChar);
            }
        }
        return false;
    }

    private Boolean getBoolean(Reader reader, ArrayList<String> fields, StringBuilder sbuf) throws IOException {
        int value;
        char propChar;// Processing double-quoted string ..
        while ( (value = reader.read()) != -1 ) {
            propChar = (char)value;
            if ( propChar == DQUOTE ) {
                // Saw another double-quote. Check if
                // another char can be read.
                reader.mark(NUMMARK);
                if ( (value = reader.read()) == -1 ) {
                    // Nope, found EOF; means End of
                    // field, End of record and End of
                    // File
                    getFileLength(fields, sbuf);
                    return true;
                } else if ( (propChar = (char)value) == DQUOTE ) {
                    // Found a second double-quote
                    // character. Means the double-quote
                    // is included.
                    sbuf.append( DQUOTE );
                } else if ( propChar == CRETURN ) {
                    // Found End of line. Means End of
                    // field, and End of record.
                    getFileLength(fields, sbuf);
                    // Read and discard a line-feed if we
                    // can indeed do so.
                    return discardLinefeed( reader,
                            stripMultipleNewlines );
                } else if ( propChar == LFEED ) {
                    // Found end of line. Means End of
                    // field, and End of record.
                    getFileLength(fields, sbuf);
                    // No need to check further. At this
                    // point, we have not yet hit EOF, so
                    // we return false.
                    return isStripMultipleNewLines(reader);
                } else {
                    // Not one of EOF, double-quote,
                    // newline or line-feed. Means end of
                    // double-quote processing. Does NOT
                    // mean end-of-field or end-of-record.
                    // System.err.println("EOR on '" + c +
                    reader.reset();
                    break;
                }
            } else {
                // Not a double-quote, so no special meaning.
                sbuf.append( propChar );
            }
        }
        // Hit EOF, and did not see the terminating double-quote.
        if ( value == -1 ) {
            // We ignore this error, and just add whatever
            // left as the next field.
            getFileLength(fields, sbuf);
            return true;
        }
        return false;
    }

    private Boolean isStripMultipleNewLines(Reader reader) throws IOException {
        if ( stripMultipleNewlines )
            return discardLinefeed( reader, stripMultipleNewlines );
        else return false;
    }

    private void getFileLength(ArrayList<String> fields, StringBuilder sbuf) {
        if ( sbuf.length() > 0 ) {
            fields.add( sbuf.toString() );
            sbuf.delete( 0, sbuf.length() );
        }
    }
}
