__author__ = 'kingkz'

import codecs


from elasticsearch import Elasticsearch

INDEX_NAME = "homework3"
DOC_TYPE = 'VerticalSearch'

in_link = {}


def write_inlinks():
    with codecs.open('in_links_crawled.txt', 'w', 'utf-8') as in_link_file:
        for link in in_link:
            result = link + ' '
            result += ' '.join(in_link[link])
            # print result
            in_link_file.write(result)
            in_link_file.write('\n')

def main():
    with codecs.open('out_links_crawled.txt', 'w', 'utf-8') as out_link_file:
        query = {'query': {'match_all': {}}}
        es = Elasticsearch()
        scanResp= es.search(index=INDEX_NAME, doc_type=DOC_TYPE, body=query, search_type="scan", scroll="10m", _source_include=["out_links"])
        scrollId= scanResp['_scroll_id']

        count = 0
        while True:
            response = es.scroll(scroll_id=scrollId, scroll= "10m")
            resp = response['hits']['hits']
            scrollId = response['_scroll_id']

            for hit in resp:
                url = hit['_id']
                out_links = hit['_source']['out_links']
                try:
                    result = url + ' '
                    result += ' '.join(out_links)
                    # print result
                    out_link_file.write(result)
                    out_link_file.write('\n')

                    if type(out_links[0]) == 'list':
                        out_links = out_links[0]

                    for link in out_links:
                        if link in in_link:
                            in_link[link].add(url)
                        else:
                            in_link[link] = set([url])
                except:
                    # print out_links
                    # print hit['_source']['author']
                    pass

                count += 1
                if count % 1000 == 0:
                    print count

            if not resp:
                break

    write_inlinks()


if __name__ == '__main__':
    main()


