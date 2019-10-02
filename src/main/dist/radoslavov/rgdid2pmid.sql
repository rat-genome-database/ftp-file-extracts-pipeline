select xdb.RGD_ID as REF_RGD_ID, xdb.ACC_ID as PMID from RGD_ACC_XDB xdb, REFERENCES ref
where xdb.XDB_KEY=2 and
xdb.RGD_ID=ref.RGD_ID
