package org.dict_uk.expand

import groovy.transform.CompileStatic
import java.util.regex.Pattern

class TaggedWordlist {


def extra_tag_map = [
  "base-abbr.lst": ":abbr",
  "dot-abbr.lst": ":abbr",
  "twisters.lst": ":bad",
  "ignored.lst": ":bad",
  "rare.lst": ":rare",
  "slang.lst": ":slang",
  "alt.lst": ":alt"
]



def process_line_exceptions(line) {

    if( ! line.contains(" ") || line ==~ ".*[а-яіїєґА-ЯІЇЄҐ] /.*" )
        return [line]
      
    if( line ==~ /[^ ]+ [^ ]+ [^:]?[a-z].*/ )
        return [line]

	return line
}

Pattern comment_re = ~ / *#.*$/
Pattern lemma_tag_re1 = ~ /^[^ ]+ [:^<a-z0-9_].*$/
Pattern lemma_tag_re2 = ~ /^([^ ]+) ([^<a-z].*)$/
Pattern with_flags_re = ~ '^[а-яіїєґА-ЯІЇЄҐ\'-]+ /'
Pattern word_lemma_tag_re = ~ /^[^ ]+ [^ ]+ [^:]?[a-z].*$/

@CompileStatic
def process_line(String line, String extra_tags) {
	String comment = null
	def commentMatcher = comment_re.matcher(line)
	if( commentMatcher.find() ) {
		line = comment_re.matcher(line).replaceFirst("") // remove comments
		comment = commentMatcher.group(0)
		
		comment = comment.replaceAll(/\s*#(>.*| *TODO.*|\s*?(:?past|:?pres|rv_...|-ший)[^#]*)/, '')
		if( ! comment.trim() ) {
			comment = null
		}
	}
    
	def out_line
    if( ! line.contains(" ") \
    		|| with_flags_re.matcher(line) \
    		|| word_lemma_tag_re.matcher(line) ) {
        out_line = line
    }
    else if( lemma_tag_re1.matcher(line) ) {
        out_line = lemma_tag_re2.matcher(line).replaceFirst('$1 $1 $2')
    }
    else {
        assert false, "hit unknown tag line: >>" + line + "<<"
    }
			
    //if extra_tags != "" && not re.match(".* [a-z].*$", out_line):
    if( extra_tags != "" && (! (out_line =~ / [:a-z]/) || out_line.contains("g=")) ) {
        extra_tags = " " + extra_tags
    }
    else if( line.startsWith(" +") ) {
        extra_tags = ""
    }
      
    if( ! out_line.contains("tag=") && out_line.contains("|") ) {
        out_line = out_line.replace("|", extra_tags + "|")
    }
    
    out_line = out_line + extra_tags
    if( out_line.contains(" \\ ") ) {
        out_line = out_line.replace(" \\ ", " ") + " \\"
    }
    else if( out_line.contains(" \\:") ) {
        out_line = out_line.replace(" \\:", ":") + " \\"
    }

	if( comment ) {
		out_line += comment
	}
	      
    return out_line
}


List<String> process_input(List<String> files) {
    List<String> out_lines = []
    for(String filename in files) {

		def detectProperNoun = false
		
        def fn = new File(filename).name
		
		def extra_tags
        if( fn in extra_tag_map ) {
            extra_tags = extra_tag_map[fn]
        }
        else {
            extra_tags = ""
        }

		if( fn =~ /name.*\.lst|alt.lst|geo.*\.lst/ ) {
			detectProperNoun = true
		}
    
        new File(filename).withReader("utf-8") { reader ->
            for( line in reader ) {
//				System.err.println("line: " + line)

			    line = line.replaceAll(/ +$/, "")
                if( line ==~ / *(#.*)?/ )
                    continue
                
                if( line.startsWith(" +") ) {
                    if( extra_tags )
                        line += " " + extra_tags
                    out_lines.add( line )
                    continue
                }
				
                if( filename.endsWith( "exceptions.lst" ) ) {
                    def lines = process_line_exceptions(line)
                    if( lines )
                        out_lines.addAll( lines )
                }
			    else {
					
					def extra_tags2 = extra_tags;
					
					if( detectProperNoun ) {
						if( Character.isUpperCase(line.charAt(0))
								&& ( (line.contains(" /n") && ! line.contains("<") )
									|| (line.contains(" noun") && line.contains(":nv")) ) ) {
							extra_tags2 += ":prop"
						}
					}
					else if ( fn == "base.lst" ) {
						if( line.contains(":fname") ) {
							line = line.replace(":prop:fname", "")
						}
					}
					
                    def out_line = process_line(line, extra_tags2)
                    if( out_line.trim() )
                        out_lines.add( out_line )
                }
            }
        }
    }
    return out_lines
}


}