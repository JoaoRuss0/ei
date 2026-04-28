# Derived after the cluster is initiated - we need the public dns of each (count var) instance on the cluster
locals {
    replica_directorys = ["r1eMpKZRROex80kgn4_2-g", "bWIe6tPFS3mKvq-W-_kgtQ", "Z57HkCAASdifFa-QWCdGWA"]
    quorum_voters = join(",", [
        for i, instance in aws_instance.exampleCluster :
        "${i + 1}@${instance.public_dns}:9093:${local.replica_directorys[i]}"
    ])
}
