import urllib2
import sqlite3

if  __name__ =='__main__':
    unidata = urllib2.urlopen("http://www.unicode.org/Public/UNIDATA/NamesList.txt")

    conn = sqlite3.connect(r'..\resources\unicode.db')
    c = conn.cursor()    
    c.execute("DROP TABLE IF EXISTS unicode") # Clear previous table, if it existed
    c.execute("CREATE TABLE unicode (char integer, description text, block text)")

    table = ""
    count = 0
    for line in unidata:
        values = line.split('\t')
        if line.startswith("@@\t"):
            table = values[2]
            print "Reading block %s..." % table
            continue    
        if line and line[0] in '\t@;' or values[1].startswith('<'):
            continue
        c.execute("INSERT INTO unicode VALUES (?, ?, ?)", ((int(values[0], 16), values[1], table)))
        count += 1
    print "Read %d entries." % count
    conn.commit()
    conn.close()