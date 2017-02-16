from operator import itemgetter
from sklearn import tree

__author__ = 'kingkz'

import config
import elasticsearch

es = elasticsearch.Elasticsearch()

feature_set = ['click here', 'sex', 'free', 'sex', 'porn', 'erection']


def get_qrel():
    qrel_dir = config.DATA_HOME + config.QREL_FILE

    qrel_map = {}

    train_qrel_map = {}

    test_qrel_map = {}

    with open(qrel_dir) as reader:
        for line in reader.readlines():
            line = line.strip()
            rel, file_ = line.split(' ')
            if rel == 'spam':
                rel = 1
            else:
                rel = 0
            email = file_.split('/')[-1]
            qrel_map[email] = rel

    query = {
        "query": {
            "match_all": {}
        }
    }

    scanResp = es.search(index=config.INDEX, doc_type=config.DOC_TYPE, body=query,
                         scroll='10m', fields=['class'])
    hits = scanResp['hits']['hits']

    while hits:
        for hit in hits:
            print hit['fields']['class']
            if hit['fields']['class'][0] == 'train':
                train_qrel_map[hit['_id']] = qrel_map[hit['_id']]
            else:
                test_qrel_map[hit['_id']] = qrel_map[hit['_id']]
        scrollId = scanResp['_scroll_id']
        scanResp = es.scroll(scroll_id=scrollId, scroll='10m')
        hits = scanResp['hits']['hits']

    return qrel_map, train_qrel_map, test_qrel_map


def get_feature_map(keyword):
    query = {
        "query": {
            "match_phrase": {
                "text": {
                    "query": keyword,
                    "analyzer": "search_grams"
                }
            }
        }
    }

    scanResp = es.search(index=config.INDEX, doc_type=config.DOC_TYPE, body=query,
                         scroll='10m', fields=[])
    hits = scanResp['hits']['hits']

    res = {}
    while hits:
        for hit in hits:
            res[hit['_id']] = hit['_score']
        scrollId = scanResp['_scroll_id']
        scanResp = es.scroll(scroll_id=scrollId, scroll='10m')
        hits = scanResp['hits']['hits']

    return res

def get_feature_matrix(qrel_map, train_qrel_map):
    all_feature_map = {}

    for feature in feature_set:
        all_feature_map[feature] = get_feature_map(feature)

    train_matrix = []
    train_label = []
    train_emails = []

    test_matrix = []
    test_label = []
    test_emails= []

    for email in qrel_map:
        row = []
        for feature in feature_set:
            if email in all_feature_map[feature]:
                row.append(all_feature_map[feature][email])
            else:
                row.append(0)
        if email in train_qrel_map:
            train_matrix.append(row)
            train_label.append(qrel_map[email])
            train_emails.append(email)
        else:
            test_matrix.append(row)
            test_label.append(qrel_map[email])
            test_emails.append(email)

    return train_matrix, train_label, test_matrix, test_label, train_emails, test_emails


def run_decision_tree(qrel_map, train_qrel_map):

    train_data, train_label, test_data, test_label, train_emails, test_emails = get_feature_matrix(qrel_map, train_qrel_map)

    clf = tree.DecisionTreeClassifier()
    clf.fit(train_data, train_label)

    train_result = clf.predict(train_data).tolist()
    test_result = clf.predict(test_data).tolist()

    train_res, test_res = analyze_result(train_result, train_emails, test_result, test_emails)

    print_first_spams(test_res)


def print_first_spams(test_res):
    sorted_lst = sorted(test_res.items(), key=itemgetter(1), reverse=True)
    for i in range(50):
        print sorted_lst[i]



def analyze_result(train_raw, train_emails, test_raw, test_emails):
    train = {}
    test = {}

    for i, key in enumerate(train_emails):
        train[key] = train_raw[i]

    for i, key in enumerate(test_emails):
        test[key] = test_raw[i]

    return train, test




def main():
    pass


if __name__ == '__main__':
    qrel_map, train_qrel_map, test_qrel_map = get_qrel()
    run_decision_tree(qrel_map, train_qrel_map)