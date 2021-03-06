SELECT DISTINCT
    fa.RGD_ID AS gene_id,
    ref_info.RGD_ID,
    ref_info.author_names AS authors,
    ref_info.citation,
    acc_id AS PMID
FROM
    rgd_ids ri,
    rgd_ref_rgd_id fa,
    (
        SELECT
            rra.ref_key                                                                                                  AS ref_key,
            listagg(concat(concat(au.AUTHOR_LNAME,', '),au.AUTHOR_FNAME), '; ') within GROUP (ORDER BY rra.AUTHOR_ORDER) AS author_names ,
            ref.CITATION,
            ref.RGD_ID,
            xdb.ACC_ID
        FROM
            rgd_ref_author rra,
            AUTHORS au,
            REFERENCES ref
        LEFT JOIN
            rgd_acc_xdb xdb
        ON
            (ref.RGD_ID=xdb.RGD_ID AND xdb.XDB_KEY=2)
        WHERE
            rra.AUTHOR_KEY=au.AUTHOR_KEY
        AND rra.REF_KEY=ref.REF_KEY
        GROUP BY
            rra.REF_KEY,
            ref.CITATION,
            ref.RGD_ID,
            xdb.ACC_ID ) ref_info
WHERE
    fa.RGD_ID = ri.RGD_ID
AND ri.OBJECT_KEY=1
AND fa.ref_key=ref_info.ref_key
ORDER BY
    ref_info.rgd_id ;

