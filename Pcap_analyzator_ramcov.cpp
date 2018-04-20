
#include "stdafx.h"
#include <stdio.h>
#include <pcap.h>
#include <Winsock2.h>
#include <string.h>

#define LINE_LEN 16

#define EH 14  //Ethernet header

#define IP_PROTOCOL_FIELD 23
#define SRC1  34
#define SRC2  35
#define DEST1 36
#define DEST2 37

typedef struct frames {
	int size;
	char ip[15];
	unsigned char *start;
}FRAMES;

typedef struct ramec {
	int size;
	int rank;
	unsigned char *start;
	struct ramec *next;
}RAMEC;

typedef struct port {
	int number;
	char name[100];
	struct port *next;
}PORT;

typedef struct protocol {
	int number;
	char name[100];
	struct protocol *next;
}PROTOCOL;

char first_ten_ip[10][15];
char last_ten_ip[10][15];
int first_ten_ip_check[10] = { 0,0,0,0,0,0,0,0,0,0};
int last_ten_ip_check[10] = { 0,0,0,0,0,0,0,0,0,0 };
FRAMES ramec[10];
int index_ramca = 0;
int counter_ramca = 0;
PORT *ports;
PROTOCOL *protocol;
int max_frame_size=0;
char max_frame_ip[15];
RAMEC *list;
int counter = 0;

void dispatcher_handler(u_char *, const struct pcap_pkthdr *, const u_char *);

void find_communication(char *name);

int find(char *name);

void add_to_list(const struct pcap_pkthdr *header, const u_char *pkt_data,int rank);

void packet_content(const u_char *pkt_data, int len);

void mac_adresy(const u_char *pkt_data);

char* get_protocol_name(int number);

char* get_port_name(int number);

int main(int argc, char ** argv);

int ethernet_type(int x, int y, int z,int eth2);

void arp_pairs();

void load_prot_port(char *f_name);

void ipv4(const u_char *pkt_data);

void  frames_and_len(int size);

void icmp(int type);

int main(int argc, char **argv)
{

	list = (RAMEC*)malloc(sizeof(RAMEC));
	list->next = NULL;
	int list_len;
	pcap_t *fp;
	char errbuf[PCAP_ERRBUF_SIZE];
	char source[PCAP_BUF_SIZE];
	char *path = "C:\\Users\\pcap\\trace-24.pcap"; //  C:\\Users\\jojdz\\Desktop\\test_cvicenia.pcap
	char *prot_port_path = "C:\\Users\\jojdz\\Desktop\\7semester\\PKS\\AfterReinstall\\PKS_projekt\\protocols_and_ports.txt";

	load_prot_port(prot_port_path);

	printf("Chcete nacitat novu cestu pre pcap file? y/n\n");
	if (getchar() == 'y') {
		//free(path);
		path = (char*)malloc(100 * sizeof(char));
		scanf("%s", path);
	}

	/* Create the source string according to the new WinPcap syntax */
	if (pcap_createsrcstr(source,PCAP_SRC_FILE,NULL,NULL,path,errbuf) != 0)
	{
		fprintf(stderr, "\nError creating a source string\n");
		return -1;
	}

	/* Open the capture file */
	if ((fp = pcap_open(source,65536,PCAP_OPENFLAG_PROMISCUOUS,1000,NULL,errbuf)) == NULL)
	{
		fprintf(stderr, "\nUnable to open the file %s.\n", source);
		return -1;
	}
	// -----------------------Reading pcap API--------------------------------------------
	pcap_loop(fp, 0, dispatcher_handler, NULL);
	/*
	ak bolo packetov napr 42 frame counter bude 1, co je realne 2 index v poli
	*/
	//------------------------Priprava na analyzu ramcov-------------------------
	int first_ten_counter;

	if (counter_ramca > 10)
		first_ten_counter = 10;
	else
		first_ten_counter = counter_ramca;

	int j;
	list_len = counter_ramca;
	if (counter_ramca < 20)
	{
		j = 0;
	}
	else
	{
		j = index_ramca;
	}
	//ak mam 25 ramcov, j bude mat hodnotu  
	//5 teda zacinam vypisovat ramcom v poli cislo 5, teda tak isto buedm zacinat vypisvovat aj ipcky
	int last_frame_counter = j;
	counter_ramca -= 10;
	//--------------------Analyza 10 alebo menej poslednych ramcov---------------------------
	while (ramec[j].size > 0)
	{
		++counter_ramca;
		int ether_type = (ramec[j].start[12] << 8) + ramec[j].start[13];

		frames_and_len(ramec[j].size);//vypis ramec, dlzka api a po mediu

		if (ethernet_type(ether_type, ramec[j].start[14], ramec[j].start[15],find("ETHERNET_2")) == 1)
		{
			if (ether_type == find("IPV4")) {//IPv4
				//spritnf preto tu lebo sa mi nechce osetrovat vo funkcii pre prve alebo posledne ip -_-
				sprintf(last_ten_ip[j], "%d.%d.%d.%d", ramec[j].start[12 + EH], ramec[j].start[13 + EH], ramec[j].start[14 + EH], ramec[j].start[15 + EH]);
				last_ten_ip_check[j] = 1;
				ipv4((const u_char*)ramec[j].start);
			}
			if (ether_type == find("ARP")) {//ARP
				sprintf(last_ten_ip[j], "%d.%d.%d.%d", ramec[j].start[38], ramec[j].start[39], ramec[j].start[40], ramec[j].start[41]);
				last_ten_ip_check[j] = 1;
			}
		}
		//check najdlhsieho ramca
		if (ramec[j].size > max_frame_size && last_ten_ip_check[j] != 0) {
			max_frame_size = ramec[j].size;
			strcpy(max_frame_ip, last_ten_ip[j]);
		}

		mac_adresy((const u_char*)ramec[j].start);

		packet_content((const u_char *)ramec[j].start, ramec[j].size);

		//pre while cyklus
		ramec[j].size = 0;
		j = (j + 1) % 10;
	}

	//-----------------END, IP analyzovanych paketov-----------------------
	int i;
	printf("IP adresy vysielajucich uzlov: \n");
	//first ten
	for (i = 0;i < first_ten_counter;i++) {
		if (first_ten_ip_check[i] == 1)
			printf("%s\n", first_ten_ip[i]);
	}
	int pom = 1;
	if (i == 10) {//iba ak je viac ako 10
		//printf("Nasleduje poslednych 10: \n");
		for (i = last_frame_counter;pom == 1 || i != last_frame_counter;i = (i + 1) % 10) {
			pom = 0;
			if(strlen(last_ten_ip[i])!=0 && last_ten_ip_check[i] == 1)
				printf("%s\n", last_ten_ip[i]);
		}
	}

	printf("\n\n");
	printf("IP adresa s najvacsim poctom odvysielanych bytov\n");
	printf("IP: %s \t %d B\n", max_frame_ip, max_frame_size);
//-------------------------------BOD 3, analyza konkretnych protokolov a portov----------------------------------
	printf("Nacitajne pocet HTTP komunikacii na vypis, 0 pre vsetky\n");
	scanf("%d", &counter);
	if (counter == 0)counter = list_len;
	find_communication("HTTP");

	printf("Nacitajne pocet HTTPS komunikacii na vypis, 0 pre vsetky\n");
	scanf("%d", &counter);
	if (counter == 0)counter = list_len;
	find_communication("HTTPS");

	printf("Nacitajne pocet TELNET komunikacii na vypis, 0 pre vsetky\n");
	scanf("%d", &counter);
	if (counter == 0)counter = list_len;
	find_communication("TELNET");

	printf("Nacitajne pocet SSH komunikacii na vypis, 0 pre vsetky\n");
	scanf("%d", &counter);
	if (counter == 0)counter = list_len;
	find_communication("SSH");

	printf("Nacitajne pocet FTP_riadiace komunikacii na vypis, 0 pre vsetky\n");
	scanf("%d", &counter);
	if (counter == 0)counter = list_len;
	find_communication("FTP_riadiace");

	printf("Nacitajne pocet FTP_datove komunikacii na vypis, 0 pre vsetky\n");
	scanf("%d", &counter);
	if (counter == 0)counter = list_len;
	find_communication("FTP_datove");

	printf("Nacitajne pocet TFTP komunikacii na vypis, 0 pre vsetky\n");
	scanf("%d", &counter);
	if (counter == 0)counter = list_len;
	find_communication("TFTP");

	printf("Nacitajne pocet ICMP komunikacii na vypis, 0 pre vsetky\n");
	scanf("%d", &counter);
	if (counter == 0)counter = list_len;
	find_communication("ICMP");

	printf("Nacitajne pocet ARP dvojic na vypis, 0 pre vsetky\n");
	scanf("%d", &counter);
	if (counter == 0)counter = list_len;
	arp_pairs();

	getchar();
	getchar();
	return 0;
}

/*
Funkcia prechadza list, zistuje ci je packet IPv4, ak ano najprv pozrie ci je v ipv4 vnoreny protokol zo suboru, ak je
prechadza protokly ktore sa rozoberaju a postupne ich kontroluje
ak nie je, prechadza na porty. Ulozi si cisla portov a nasledne porovna cisla dvoch portov s cislami portov zo suboru a vypise informacie o packete
*/
void find_communication(char *name) {
	
	RAMEC *akt;
	akt = list;
	char *port1 = (char*)malloc(100 * sizeof(char));
	char *port2 = (char*)malloc(100 * sizeof(char));
	while (akt->next != NULL && counter != 0) {
		int ether_type = (akt->start[12] << 8) + akt->start[13];
		//musi to byt ipv4
		if (ether_type == find("IPV4") && (akt->start[14] >> 4) == 4) {
			port1 = get_port_name(akt->start[SRC1] * 256 + akt->start[SRC2]);
			port2 = get_port_name(akt->start[DEST1] * 256 + akt->start[DEST2]);
			int ipLen = (akt->start[14] << 8) >> 8;
			//je to protokol za ipv4
			if (strcmp(get_protocol_name(akt->start[23]), name) == 0) {//funkcia dostane cislo najde podla neho meno
				if (strcmp(name, "ICMP") == 0) {
					//vypis ICMP
					counter--;
					printf("ramec %d\n", akt->rank);
					icmp(akt->start[34]);
					ethernet_type(ether_type, akt->start[14], akt->start[15], find("ETHERNET_2"));
					mac_adresy(akt->start);
					ipv4((const u_char*)akt->start);
					packet_content(akt->start, akt->size);
				}
			}
			//check portov bud SRC alebo DEST
			else if ((port1 != NULL && strcmp(port1, name) == 0) || (port2 != NULL && strcmp(port2, name) == 0)) {
				counter--;
				if (strcmp(name, "FTP_riadiace") == 0) {
					printf("ramec %d\n", akt->rank);
					ethernet_type(ether_type, akt->start[14], akt->start[15], find("ETHERNET_2"));
					mac_adresy(akt->start);
					ipv4((const u_char*)akt->start);
					packet_content(akt->start, akt->size);

				}
				if (strcmp(name, "FTP_datove") == 0) {
					printf("ramec %d\n", akt->rank);
					ethernet_type(ether_type, akt->start[14], akt->start[15], find("ETHERNET_2"));
					mac_adresy(akt->start);
					ipv4((const u_char*)akt->start);
					packet_content(akt->start, akt->size);
				}
				if (strcmp(name, "SSH") == 0) {
					printf("ramec %d\n", akt->rank);
					ethernet_type(ether_type, akt->start[14], akt->start[15], find("ETHERNET_2"));
					mac_adresy(akt->start);
					ipv4((const u_char*)akt->start);
					packet_content(akt->start, akt->size);
				}
				if (strcmp(name, "TELNET") == 0) {
					printf("ramec %d\n", akt->rank);
					ethernet_type(ether_type, akt->start[14], akt->start[15], find("ETHERNET_2"));
					mac_adresy(akt->start);
					ipv4((const u_char*)akt->start);
					packet_content(akt->start, akt->size);
				}
				if (strcmp(name, "TFTP") == 0) {
					printf("ramec %d\n", akt->rank);
					ethernet_type(ether_type, akt->start[14], akt->start[15], find("ETHERNET_2"));
					mac_adresy(akt->start);
					ipv4((const u_char*)akt->start);
					packet_content(akt->start, akt->size);
				}
				if (strcmp(name, "HTTP") == 0) {
					printf("ramec %d\n", akt->rank);
					ethernet_type(ether_type, akt->start[14], akt->start[15], find("ETHERNET_2"));
					mac_adresy(akt->start);
					ipv4((const u_char*)akt->start);
					packet_content(akt->start, akt->size);
				}
				if (strcmp(name, "HTTPS") == 0) {
					printf("ramec %d\n", akt->rank);
					ethernet_type(ether_type, akt->start[14], akt->start[15], find("ETHERNET_2"));
					mac_adresy(akt->start);
					ipv4((const u_char*)akt->start);
					packet_content(akt->start, akt->size);
				}
			}
		}
		akt = akt->next;
	}
}

/*
funkcia na zistenie typu ethernetu, vracia 1 ak je to ethernet 2, teda ak sa ide rozoberat, inak vracia 0
*/
int ethernet_type(int x, int y, int z,int eth2) {

	if (x > eth2)//ak je nad 1500
	{
		printf("Ethernet II\n");
		return 1;
	}
	else
	{
		//printf("Novell 802.3"); 
		if (y == 0xFF && z == 0xFF)
			printf("Novel 802.3 RAW\n");
		else if (y == 0xAA && z == 0xAA)
			printf("IEEE 802.3 - SNAP\n");
		else printf("IEEE 802.3 - LLC\n");
	}
	return 0;
}

void dispatcher_handler(u_char *param, const struct pcap_pkthdr *header, const u_char *pkt_data)
{
	
	int ether_type = (pkt_data[12] << 8) + pkt_data[13];// Ether type je definovany 2 bajtmi
	char *protocol_name = (char*)malloc(100 * sizeof(char));
	//int snap_raw_llc = pkt_data[14] << 8 + pkt_data[15];
	++counter_ramca;
	add_to_list(header, pkt_data,counter_ramca);
	int tcpLen = 0;

	if (counter_ramca <= 10)
	{
		
		frames_and_len(header->len);//vypis counter, dlzka po api a po mediu
		//zistenie ci je to Ethernet 2
		if (ethernet_type(ether_type, pkt_data[14], pkt_data[15], find("ETHERNET_2")) == 1) {//analyza pre Ethernet 2 a TCP/IPv4
			//zistenie ci je to ipv4 a vypis pre ipv4
			if (ether_type == find("IPV4")) {
				sprintf(first_ten_ip[counter_ramca - 1], "%d.%d.%d.%d", pkt_data[12 + EH], pkt_data[13 + EH], pkt_data[14 + EH], pkt_data[15 + EH]);
				ipv4(pkt_data);
				first_ten_ip_check[counter_ramca - 1] = 1;
			}
			//zistenie ARP, rozdiel v ip polohe v ramci
			if (ether_type == find("ARP")) {//ARP
				sprintf(first_ten_ip[counter_ramca - 1], "%d.%d.%d.%d", pkt_data[38], pkt_data[39], pkt_data[40], pkt_data[41]);
				first_ten_ip_check[counter_ramca - 1] = 1;
			}
		}
			//check for longest ip
			if (header->len > max_frame_size && first_ten_ip_check[counter_ramca - 1] == 1) {
				strcpy(max_frame_ip, first_ten_ip[counter_ramca - 1]);
				max_frame_size = header->len;
			}
		mac_adresy(pkt_data);

		//vypis ramca
		packet_content(pkt_data, header->caplen);

	}

	else
	{
		
		//Uvolni pole ak bolo vytvorene(kruhovy buffer) a vytvori nanovo
		
		ramec[index_ramca].size = header->caplen;
		if (ramec[index_ramca].start != NULL)
		{
			free(ramec[index_ramca].start);
		}
		ramec[index_ramca].start = (unsigned char*)malloc(header->caplen);
		for (int i = 0; i < header->caplen; i++)
		{
			ramec[index_ramca].start[i] = pkt_data[i];
		}
		index_ramca++;

		if (index_ramca >= 10)
			index_ramca = 0;

	}
}

void ipv4(const u_char *pkt_data) {
	char *protocol_name = (char*)malloc(100 * sizeof(char));
	int ipv4len = (pkt_data[14] & 0x0F) * 4;//ak je ihl 5 je to ok, optionals nie su doplnene
	//src 12 13 14 15
	//dest 16 17 18 19
	//save src adress- vyskladaj ip string
	printf("IPv4\n");
	//zisti protokol
	//vypis ip adries
	printf("zdrojova IP adresa: %d.%d.%d.%d\n", pkt_data[12 + EH], pkt_data[13 + EH], pkt_data[14 + EH], pkt_data[15 + EH]);
	printf("cielova  IP adresa: %d.%d.%d.%d\n", pkt_data[16 + EH], pkt_data[17 + EH], pkt_data[18 + EH], pkt_data[19 + EH]);
	protocol_name = get_protocol_name(pkt_data[IP_PROTOCOL_FIELD]);
	if (protocol_name != NULL) {
		if (strcmp(protocol_name, "ICMP") == 0) {
			//rozbor icmp
			icmp(pkt_data[EH + ipv4len]);
		}
		if (strcmp(protocol_name, "TCP") == 0) {
			printf("TCP\n");
			//check dalsich protokolov, vypis ipcky jednej aj druhej, IPV4 porty
			printf("zdrojovy port %d\n", pkt_data[EH + ipv4len] * 256 + pkt_data[EH + ipv4len + 1]);
			printf("cielovy port %d\n", pkt_data[EH + ipv4len + 2] * 256 + pkt_data[EH + ipv4len + 3]);
		}
		if (strcmp(protocol_name, "UDP") == 0) {
			printf("UDP\n");
			//check dalsich protokolov, vypis ipcky jednej aj druhej, IPV4 porty
			printf("zdrojovy port %d\n", pkt_data[EH + ipv4len] * 256 + pkt_data[EH + ipv4len + 1]);
			printf("cielovy port %d\n", pkt_data[EH + ipv4len + 2] * 256 + pkt_data[EH + ipv4len + 3]);
		}
	}
}
//Vypis mac adries ramca
void mac_adresy(const u_char *pkt_data) {
	int i;
	printf("Zdrojova MAC adresa: ");
	for (int i = 6; i < 12; i++)
		printf("%02X ", pkt_data[i]);
	printf("\n");

	printf("Cielova MAC adresa: ");
	for (int i = 0; i < 6; i++)
		printf("%02X ", pkt_data[i]);
	printf("\n");
}
//Vypis informacii na zaklade dlzky ramca a jeho poradie
void frames_and_len(int size) {
	printf("ramec %d\n", counter_ramca);
	printf("dlzka ramca poskytnuta pcap API - %d B\n", size);
	int dlzka_ramca_po_mediu;
	if (size < 60)
		dlzka_ramca_po_mediu = 64;
	else dlzka_ramca_po_mediu = size + 4;
	printf("dlzka ramca prenasaneho po mediu - %d B\n", dlzka_ramca_po_mediu);
}

//Funkcia na ziskanie mena protokolu podla cisla
char *get_protocol_name(int number) {
	PROTOCOL *akt;
	akt = protocol;
	while (akt->next != NULL) {
		if (akt->number == number)
			return akt->name;
		else akt = akt->next;
	}
	return NULL;
}
//Funkcia na ziskanie mena portu podla cisla
char *get_port_name(int number) {
	PORT *akt;
	akt = ports;
	while (akt->next != NULL) {
		if (akt->number == number)
			return akt->name;
		else akt = akt->next;
	}
	return NULL;
}
/*
Nacitanie protokolov a portov, robene v takomto style do 2 listov kvoli moznemu opakovaniu cisel, a prehladnosti
*/
void load_prot_port(char *f_name) {
	
	FILE *f = fopen(f_name, "r");
	int numb = 0;
	char name[100];
	PROTOCOL *akt;
	protocol = (PROTOCOL *)malloc(sizeof(PROTOCOL));
	protocol->next = NULL;
	akt = protocol;
	while (fscanf(f,"%d ", &numb) > 0 && (numb != -1) && fscanf(f,"%s", name) > 0)//zlom medzi protocolmi a portami je -1
	{
		strcpy(akt->name, name);
		akt->number = numb;
		akt->next = (PROTOCOL *)malloc(sizeof(PROTOCOL));
		akt = akt->next;
		akt->next = NULL;
	}

	ports= (PORT *)malloc(sizeof(PORT));
	PORT *akt_port = ports;
	akt_port->next = NULL;
	while (fscanf(f,"%d ", &numb)>0 && fscanf(f,"%s", name) > 0)
	{
		strcpy(akt_port->name, name);
		akt_port->number = numb;
		akt_port->next = (PORT *)malloc(sizeof(PORT));
		akt_port = akt_port->next;
		akt_port->next = NULL;
	}
	fclose(f);
}
//Najdenie protokolu alebo portu podla mena
int find(char *name) {
	PROTOCOL *akt;
	akt = protocol;
	while (strcmp(name, akt->name) != 0) {
		akt = akt->next;
		if (akt->next == NULL)
			break;
	}
	if(strcmp(name, akt->name) == 0)
		return akt->number;
	PORT *aktp;
	aktp = ports;
	while (strcmp(name, aktp->name) != 0) {
		aktp = aktp->next;
		if (aktp->next == NULL)
			break;
	}
	return aktp->number;
}
//Vypis obsahu ramca
void packet_content(const u_char *pkt_data, int len) {

	for (int i = 0; i < len; i++)
	{
		if (i % 8 == 0) {
			printf("  ");
			if ((i % 16) == 0) {
				printf("\n");
			}
		}
		printf("%02X ", pkt_data[i]);
	}
	printf("\n\n");
}

void arp_pairs() {
	//pojdem whilom cez list, najdem arp request pohladam k nemu reply, vratim sa k pokracovaniu iter od tohto requestu a odpocitavam  counter
	RAMEC *request_akt, *reply_akt;
	int request_found = 0;
	request_akt = list;
	int arp_com_counter = 1;
	int req_index = 0;
	while (request_akt->next != NULL && counter>0) {
		//zistenie arp
		req_index++;
		int ether_type = (request_akt->start[12] << 8) + request_akt->start[13];
		if (ether_type != find("ARP")) {
			request_akt = request_akt->next;
			continue;
		}
		//check ci je to request 7 8
		if (request_akt->start[21] == 2) {
			request_akt = request_akt->next;
			continue;
		}
		//hladanie reply
		reply_akt = request_akt->next;
		//treba najprv najst reply kym vypisem dvojicu
		while (reply_akt->next != NULL) {
			//hladam take kde sa tu cielova rovna zdrojovej requestu MAC
			int ether_type = (reply_akt->start[12] << 8) + reply_akt->start[13];
			if (ether_type != find("ARP")) {
				reply_akt = reply_akt->next;
				continue;
			}
			if (reply_akt->start[21] == 1) {
				reply_akt = reply_akt->next;
				continue;
			}
			//compare Request src MAC a Reply dest MAC, ak su rovnake vypis ak nie continue
			if (reply_akt->start[0] != request_akt->start[6] || reply_akt->start[1] != request_akt->start[7] || reply_akt->start[2] != request_akt->start[8] ||
				reply_akt->start[3] != request_akt->start[9] || reply_akt->start[4] != request_akt->start[10] || reply_akt->start[5] != request_akt->start[11]) {
				reply_akt = reply_akt->next;
			}
			else {
				request_found = 1;
				break;// lebo ak sa vsetky rovnaju tak je to spravny reply
			}
		}
		//ak je reply v liste posledne a predsa MAC adresy nesuhlasia dvojica nie je dvojica, a nic sa nevypisuje
		if (reply_akt==NULL || reply_akt->next == NULL) {
			request_found = 0;
		}
		counter--;
		printf("Komunikacia c. %d\n", arp_com_counter++);
		//Request vypisuje v tu dole IP adresa zdrojovu adresu
		printf("ARP-request, IP adresa: %d.%d.%d.%d ", request_akt->start[38], request_akt->start[39], request_akt->start[40], request_akt->start[41]);//38 39 40 41
		printf("MAC adresa: ???\n");
		//zdroj ip ciel ip 28 29 30 31   zdroj 38 39 40 41 ciel
		printf("Zdrojova IP: %d.%d.%d.%d, ", request_akt->start[38], request_akt->start[39], request_akt->start[40], request_akt->start[41]);
		printf("Cielova IP: %d.%d.%d.%d \n", request_akt->start[28], request_akt->start[29], request_akt->start[30], request_akt->start[31]);

		printf("ramec %d\n", request_akt->rank);
		printf("dlzka ramca poskytnuta pcap API: %d\n", request_akt->size);
		printf("dlzka ramca prenasana po mediu: %d\n", (request_akt->size < 60) ? 64 : request_akt->size + 4);
		//ak to je arp je to ethernet2
		printf("Ethernet 2\n");
		mac_adresy(request_akt->start);
		packet_content(request_akt->start, request_akt->size);

		//ok teraz to je reply k tomuto requestu, ide sa vypisovat
		printf("ARP-reply ");
		if (request_found == 1) {
			printf("IP adresa: %d.%d.%d.%d, ", reply_akt->start[28], reply_akt->start[29], reply_akt->start[30], reply_akt->start[31]);
			printf("MAC adresa: ");
			for (int i = 6;i < 12;i++)
				printf("%02X ", reply_akt->start[i]);
			printf("\n");
			printf("Zdrojova IP: %d.%d.%d.%d, ", reply_akt->start[38], reply_akt->start[39], reply_akt->start[40], reply_akt->start[41]);
			printf("Cielova IP: %d.%d.%d.%d \n", reply_akt->start[28], reply_akt->start[29], reply_akt->start[30], reply_akt->start[31]);

			printf("ramec %d\n", reply_akt->rank);
			printf("dlzka ramca poskytnuta pcap API: %d\n", reply_akt->size);
			printf("dlzka ramca prenasana po mediu: %d\n", (reply_akt->size < 60) ? 64 : reply_akt->size + 4);
			//ak to je arp je to ethernet2
			printf("Ethernet 2\n");
			mac_adresy(reply_akt->start);
			packet_content(reply_akt->start, reply_akt->size);
		}
		//break;
		else printf("nenajdeny\n\n");//Ak nie je najdeny reply vypise sa "nenajdeny"
		request_akt = request_akt->next;
	}

}
//Vypis typov ICMP packetov
void icmp(int type) {
	printf("ICMP\nType ");
	switch (type) {
	case 0:
		printf("Echo reply\n");
		break;
	case 3:
		printf("Destination unreachable\n");
		break;
	case 4:
		printf("Source quench\n");
		break;
	case 5:
		printf("Redirect\n");
		break;
	case 8:
		printf("Echo\n");
		break;
	case 9:
		printf("Router advertisement\n");
		break;
	case 10:
		printf("Router selection\n");
		break;
	case 11:
		printf("Time exceeded\n");
		break;
	case 12:
		printf("Parameter problem\n");
		break;
	case 13:
		printf("Timestamp\n");
		break;
	case 14:
		printf("Timestamp\n");
		break;
	case 15:
		printf("Information Request\n");
		break;
	case 16:
		printf("Information Reply\n");
		break;
	case 17:
		printf("Address Mask Request\n");
		break;
	case 18:
		printf("Adress Mask Reply\n");
		break;
	case 30:
		printf("Traceroute\n");
		break;
	}
}
//Funkcia na pridanie jedneho packetu do listu v presnom poradi
void add_to_list(const struct pcap_pkthdr *header, const u_char *pkt_data,int rank) {
	RAMEC *akt;
	akt = list;
	while (akt->next != NULL)
		akt = akt->next;
	akt->size = header->caplen;
	akt->rank = rank;
	akt->start = (unsigned char*)malloc(akt->size * sizeof(unsigned char));
	for (int i = 0;i < akt->size;i++) {
		akt->start[i] = pkt_data[i];
	}
	akt->next = (RAMEC*)malloc(sizeof(RAMEC));
	akt->next->next = NULL;
}