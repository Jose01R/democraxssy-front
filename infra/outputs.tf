# Outputs for compute instance

output "instance-name" {
  value = oci_core_instance.backend_instance.display_name
}

output "instance-OCID" {
  value = oci_core_instance.backend_instance.id
}

output "instance-availability-domain" {
  value = oci_core_instance.backend_instance.availability_domain
}

output "instance-shape" {
  value = oci_core_instance.backend_instance.shape
}

output "instance-state" {
  value = oci_core_instance.backend_instance.state
}

output "instance-image" {
  value = data.oci_core_image.instance_image.display_name
}

output "private-ip" {
  value = data.oci_core_private_ips.backend.private_ips[0].ip_address
}

# Outputs for reserved public IP

output "public-ip-for-backend" {
  value = oci_core_public_ip.backend_ip.ip_address
}

output "ssh-command" {
  value = "ssh -i ssh-keys/democraxssy-back opc@${oci_core_public_ip.backend_ip.ip_address}"
}
