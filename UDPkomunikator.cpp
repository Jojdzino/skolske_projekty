/* UDPkomunikator.cpp : Defines the entry point for the console application.
prva sprava: (poradie)0000|(pocet fragmentov)0007|(velkost fragmentu)00000|CRC
dalsia sprava (poradie)0000spravaCRC //predpoklad dlzky crc 10 znakov

*/

#include "stdafx.h"
#include "CRC.h"
#include "UDPkomunikator.h"

#include <iomanip>  // Includes ::std::hex
#include <iostream> // Includes ::std::cout
#include <cstdint>  // Includes ::std::uint32_t
#include <iostream>
#include <winsock2.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <thread>
#include <chrono>
#include <string>
#include <cmath>
#include <windows.h>


#pragma comment(lib, "ws2_32.lib")

using namespace std;

#define BUFLEN 60000
#define CRC_ZOSTATOK2 4
#define RTT_LAN 20

struct sockaddr_in server;				/* Štruktúra informácií o servery */
struct sockaddr_in client;				/* Štruktúra informácií o clientovi */
SOCKET client_socket;					/*Nabindovany socket na clientovi, pocuvanie ack*/
SOCKET server_socket;					/*Nabindovany socket na servere, pocuvanie fragmentov*/
SOCKET sending_socket;					/*Socket na posielanie*/
clock_t akt_time, begin_time, round_trip_time;			/*Casovace ukoncenia programu*/
int begin_listen = 0;
int fragment_size = 0, fragment_count;	/*Velkosti a pcoet fragmentov jednej spravy*/
int port_server;						/*Port serveru*/
int port_klient;						/*Port klienta*/
char *client_ip;						/*Ip clienta, mam problem zistit konkretnu ip*/
char *server_ip;						/*Ip serveru*/
char *buf;								/*Buffer pre spravy*/
int ack = 0;								/*Informacia o prijati ack*/
int same_msg_counter = 0;				/*Pocitanie opakovaneho poslania/prijatia rovnakej spravy*/
string sprava;							/*Prijimana sprava, rozsirovana o fragmenty*/
int clientIP[4];						/*Clientova ip ako 4 inty*/
char filled_arr[] = { 0,0,1,1,1,1,1,1,1,1,1,1,1 };
//Nastavenie struktury klienta, pretoze pri prijimani sa zvlastne prepise
void set_client_struct() {
	client.sin_family = AF_INET;
	client.sin_port = htons(port_klient);
	client.sin_addr.S_un.S_addr = inet_addr(client_ip);
}

//Pocuvanie klienta pre ack od serveru, aby sa mi nezastavil program ak nepride
void listenClient() {//wait only for ack
	struct sockaddr pom_struct;				/*Pomocna struktura pre prijimanie ack na clientovi, neprepisovanie serveru*/
	int recv_len;
	int slen = sizeof(server);
	while (begin_listen == 0)//zakial sa nezacalo posielanie
		Sleep(100);
	while (1) {
		if ((recv_len = recvfrom(client_socket, buf, BUFLEN, 0, (struct sockaddr *) &pom_struct, &slen)) == SOCKET_ERROR)
		{
			printf("recvfrom() failed with error code : %d", WSAGetLastError());
			exit(EXIT_FAILURE);
		}
		printf("Prijata sprava v listenclient\n");
		ack = 1;
	}
	return;
}

/*
prva sprava: (poradie)0000|(client_ip max 15 znakov)|(port pocuvania klienta)|(pocet fragmentov)0007|(velkost fragmentu)00000|CRC
dalsia sprava (poradie)0000spravaCRC //predpoklad dlzky crc-4 znaky
*/
void writeClient() {//posiela spravy
	cout << "Pockajte prosim na zistenie odozvy zo serveru" << endl;
	while (begin_listen != 1)
		Sleep(100);
	while (1) {

		char choice = '\0';
		char pom[15] = { '\0' };
		int fragment_count = 0;

		std::string string_to_send("");

		printf("Sprava - s, chybny ramec - c\n");
		scanf(" %c", &choice);
		if (choice != 'c' && choice != 's')
			continue;

		cout << "Napiste vasu spravu, alebo chybny ramec, ukoncite nacitavanie enterom" << endl;
		scanf(" %[^\n]s", buf);
		std::string input(buf);

		//vytvaranie prvej spravy
		{
			//cislo spravy
			string_to_send += '\0';
			string_to_send += '\0';
			//pridavanie ip adresy
			string_to_send += clientIP[0] & 0xff;
			string_to_send += clientIP[1] & 0xff;
			string_to_send += clientIP[2] & 0xff;
			string_to_send += clientIP[3] & 0xff;

			//pridavanie port klienta na ktorom pocuva
			string_to_send += (port_klient >> 8) & 0xff;
			string_to_send += (port_klient) & 0xff;

			//pridavanie pocet fragmentov
			float value = (float)input.length() / (float)fragment_size;
			fragment_count = (int)::ceil(value);
			string_to_send += (fragment_count >> 8) & 0xff;
			string_to_send += (fragment_count) & 0xff;

			//pridavanie velkosti fragmentu
			string_to_send += (fragment_size >> 8) & 0xff;
			string_to_send += (fragment_size) & 0xff;

			//pridavanie CRC
			//cout << "String: " << string_to_send << " length " << string_to_send.length() << endl;
			std::uint32_t crc = CRC::Calculate(string_to_send.c_str(), string_to_send.length(), CRC::CRC_32());
			cout << crc << endl;

			if (choice == 'c')//ak bola zvolena moznost c pozmen CRC
				crc--;
			unsigned char bytes[4];
			bytes[0] = (crc >> 24) & 0xFF;
			bytes[1] = (crc >> 16) & 0xFF;
			bytes[2] = (crc >> 8) & 0xFF;
			bytes[3] = crc & 0xFF;
			//cout << ((crc >> 24) & 0xff) << " " << ((crc >> 16) & 0xff) << " " << ((crc >> 8) & 0xff) << " " << ((crc) & 0xff) << endl;
			string_to_send += bytes[0];
			string_to_send += bytes[1];
			string_to_send += bytes[2];
			string_to_send += bytes[3];

			cout << string_to_send << endl;
		}
		//Posielanie uvodnej spravy dokym nie je ack dosiahnute, alebo nie je pocet odoslani uvodnej spravy viac ako 3
		while (same_msg_counter <= 3) {
			cout << "sending init msg" << endl;

			if (sendto(sending_socket, string_to_send.c_str(), string_to_send.length(), 0, (struct sockaddr *) &server, sizeof(server)) == SOCKET_ERROR)
			{
				printf("sendto() failed with error code : %d", WSAGetLastError());
				exit(EXIT_FAILURE);
			}
			memset(buf, '\0', BUFLEN);
			cout << "waiting for response" << endl;
			same_msg_counter++;

			//cakanie na ack v inom threade
			Sleep(round_trip_time * 2);
			if (ack == 1)//ak prislo ack chod dalej
				break;

			printf("Nebola prijata ack %s\n", buf);
			memset(buf, '\0', BUFLEN);
		}
		//Ak bola uvodna sprava poslana viac ako 3, nastala chyba, nacitaj novu spravu
		if (same_msg_counter > 3) {
			cout << "Koniec posielania, poslane viac ako 3x" << endl;
			same_msg_counter = 0;
			continue;
		}
		//Ak bola prva sprava poslana korektne odosli ostatne fragmenty
		if (same_msg_counter < 3) {
			ack = 0;
			cout << "Prijate ack uvodnej spravy" << endl;
			memset(buf, '\0', BUFLEN);
			int neparna = 0;
			int akt_poz = 0;//aktualna pozicia v posielani fragmentov
			int msg_counter = 1;
			//posielam dokym nie je dosiahnuta dlzka spravy, alebo nejaka sprava nemala pocet odoslani viac ako 3
			while (akt_poz < input.length() || same_msg_counter > 3) {
				string_to_send.clear();
				//vkladanie cisla spravy
				string_to_send += (msg_counter >> 8) & 0xff;//---------------------------------Preco by som to posuval dolava predtym?
				string_to_send += (msg_counter) & 0xff;
				//vkladanie velkosti spravy
				int msg_size = input.substr(akt_poz, fragment_size).size();//viem ze prve a posledne 4 bajty su hlavicky 
				string_to_send += (msg_size >> 8) & 0xff;
				string_to_send += (msg_size) & 0xff;
				//vkladanie obsahu spravy
				string_to_send.append(input.substr(akt_poz, fragment_size));
				//vkladanie CRC
				std::uint32_t crc = CRC::Calculate(string_to_send.c_str(), string_to_send.length(), CRC::CRC_32());
				unsigned char bytes[4];
				bytes[0] = (crc >> 24) & 0xFF;
				bytes[1] = (crc >> 16) & 0xFF;
				bytes[2] = (crc >> 8) & 0xFF;
				bytes[3] = crc & 0xFF;
				string_to_send += bytes[0];
				string_to_send += bytes[1];
				string_to_send += bytes[2];
				string_to_send += bytes[3];
				string zly = string_to_send;
				zly.append("4");
				same_msg_counter = 0;
				//cout << string_to_send << endl;
				ack = 0;
				//posielanie jedneho fragmentu
				while (same_msg_counter < 3) {
					if (neparna == 0) {
						neparna = 1;
						if (sendto(sending_socket, zly.c_str(), zly.length(), 0, (struct sockaddr *) &server, sizeof(server)) == SOCKET_ERROR)
						{
							printf("sendto() failed with error code : %d", WSAGetLastError());
							exit(EXIT_FAILURE);
						}
						cout << "waiting for response " << msg_counter << endl;
						Sleep(round_trip_time);
					}
					else {
						neparna = 0;
						if (sendto(sending_socket, string_to_send.c_str(), string_to_send.length(), 0, (struct sockaddr *) &server, sizeof(server)) == SOCKET_ERROR)
						{
							printf("sendto() failed with error code : %d", WSAGetLastError());
							exit(EXIT_FAILURE);
						}
						cout << "waiting for response " << msg_counter << endl;
						same_msg_counter++;
						Sleep(round_trip_time);
						if (ack == 1) {//ak bolo prijate ack, nastav ack na 0, a vyskoc von, posli dalsiu spravu
							ack = 0;
							break;
						}
					}
					
				}
				msg_counter++;
				akt_poz += fragment_size;
			}
			//ak nastala nejaka chyba, vrati sa do stavu nacitavania msg
			if (same_msg_counter > 3) {
				same_msg_counter = 0;
				continue;
			}
		}
	}
	return;
}


/*
Zoberie z bufferu poslednych 10 pismen zmeni ich na int
, a porovna ich z CRC substringu z buf, od zaciatku okrem crc
*/
int checkCRC(char *buf, int len_without_crc) {//Max buffer len is BUFLEN, no need to check more or less
	char *arr = (char*)malloc(len_without_crc * sizeof(char));
	memset(arr, '\0', len_without_crc);

	std::uint32_t crc_from_buff = 0;
	crc_from_buff += ((unsigned char)buf[len_without_crc + 0] << 24);
	crc_from_buff += ((unsigned char)buf[len_without_crc + 1] << 16);
	crc_from_buff += ((unsigned char)buf[len_without_crc + 2] << 8);
	crc_from_buff += ((unsigned char)buf[len_without_crc + 3]);
	//cout << "CRC v charoch je " << endl;
	cout << (unsigned int)((buf[len_without_crc + 0] & 0xff)) << " " << (unsigned int)(buf[len_without_crc + 1] & 0xff) << " " << (unsigned int)(buf[len_without_crc + 2] & 0xff) << " " << (unsigned int)(buf[len_without_crc + 3] & 0xff) << endl;

	string str("");
	for (int i = 0;i < len_without_crc;i++)
		str += buf[i];
	//cout << "Calculating crc from string " << str << "and his length " << len_without_crc << endl;
	std::uint32_t crc = CRC::Calculate(str.c_str(), len_without_crc, CRC::CRC_32());
	//cout << "String " << str << "has crc " << crc << endl;
	if (crc_from_buff != crc)
		return 0;
	return 1;
}


//returns 0 when there is an error, or 1 if ok
int parse_first(char *buf) {
	int pom = 0;
	//Prva cast hodnota je 0 v dvoch bajtoch
	pom += buf[0] << 8;
	pom += buf[1];
	if (pom != 0)
		return 0;
	pom = 0;

	//parsing IP
	memset(client_ip, '\0', 16);
	snprintf(client_ip, 16, "%d.%d.%d.%d", (unsigned char)buf[2], (unsigned char)buf[3], (unsigned char)buf[4], (unsigned char)buf[5]);

	//parsing client port
	port_klient = 0;
	port_klient += buf[6] << 8;
	port_klient += buf[7];

	//parsing fragment count
	fragment_count = 0;
	fragment_count += buf[8] << 8;
	fragment_count += buf[9];


	//parsing fragment size
	fragment_size = 0;
	fragment_size += buf[10] << 8;
	fragment_size += buf[11];

	return 1;
}

int accept_packets() {
	int msg_size;
	int sent_ack_counter = 0;
	int packet_counter_one_msg = 0;
	int i;
	int pocuvaj_uvodnu_msg = 0;
	memset(buf, '\0', BUFLEN);
	int slen = sizeof(client);
	int recv_len;
	//cakanie na prvu spravu
	while (1) {//ak nastala chyba pri prenose
		if ((recv_len = recvfrom(server_socket, buf, BUFLEN, 0, (struct sockaddr *) &client, &slen)) == SOCKET_ERROR)//prijmem od klienta
		{
			printf("recvfrom() failed with error code : %d", WSAGetLastError());
			exit(EXIT_FAILURE);
		}
		if (buf[0] == 0 && buf[1] == 0 && buf[2] == 'k') {
			cout << "Prijaty keep alive" << endl;
			begin_time = clock();
			continue;
		}
		begin_time = clock();
		cout << "received msg" << endl;
		if (checkCRC(buf, 12))//kontrola CRC
			break;
		same_msg_counter++;
		packet_counter_one_msg++;
		if (same_msg_counter > 2)
		{
			cout << "Uvodna sprava prijata nespravne 3x, ukoncenie prijimania" << endl;
			same_msg_counter = 0;
			return -1;
		}
	}
	parse_first(buf);//zisti informacie z prvej spravy
	set_client_struct();//nastav clienta, lebo pri pocuvani sa zmenil, musim vediet kam poslat ack

	cout << "Prijate, posielam ack..." << endl;
	if (sendto(sending_socket, "", 0, 0, (struct sockaddr *) &client, sizeof(client)) == SOCKET_ERROR)
	{
		printf("sendto() failed with error code : %d", WSAGetLastError());
		exit(EXIT_FAILURE);
	}
	sent_ack_counter++;
	cout << "Poslane ack pre uvodnu msg" << endl;

	int msg_counter = 1;
	while (1)
	{
		same_msg_counter = 0;
		memset(buf, '\0', BUFLEN);
		printf("Cakam na spravu...\n");
		/*
		While prijima spravy, ak je prijata sprava s cislom, ktore uz bolo spracovane, pokracuje v pocuvani,
		ak je prijatá správa viac ako 3 krát preruší sa poèúvanie
		*/
		while (1)
		{
			if ((recv_len = recvfrom(server_socket, buf, BUFLEN, 0, (struct sockaddr *) &client, &slen)) == SOCKET_ERROR)//prijmem od klienta
			{
				printf("recvfrom() failed with error code : %d", WSAGetLastError());
				exit(EXIT_FAILURE);
			}
			if (buf[0] == 0 && buf[1] == 0 && buf[2] == 'k') {
				cout << "Prijaty keep alive" << endl;
				begin_time = clock();
				continue;
			}
			begin_time = clock();
			cout << "received msg " << buf << endl;

			if (((buf[0] << 8) + buf[1]) < msg_counter) {//ak tato sprava uz bola prijata, nastav pocitadlo rovnakych sprav na 0
				same_msg_counter = 0;
				continue;
			}
			msg_size = ((buf[2] << 8) + buf[3]);
			if (checkCRC(buf, msg_size + 4))
				break;
			packet_counter_one_msg++;
			same_msg_counter++;
			if (same_msg_counter > 4)
			{
				cout << "Fragment prijaty nespravne 4x, ukoncenie prijimania" << endl;
				return -1;
			}
		}
		set_client_struct();
		printf("Posielam ack pre spravu %d...\n", msg_counter);

		if (sendto(sending_socket, "", 0, 0, (struct sockaddr *) &client, sizeof(client)) == SOCKET_ERROR)
		{
			printf("sendto() failed with error code : %d", WSAGetLastError());
			exit(EXIT_FAILURE);
		}
		msg_counter++;

		int buflen = msg_size + 4;//len po CRC
		for (i = 4;i < buflen;i++)
			sprava += buf[i];
		if (fragment_count == msg_counter - 1) {
			printf("Prijata sprava korektne\n");
			return 1;
		}
	}
}

void listenServer() {//caka na spravy

	int slen = sizeof(client);
	cout << "Cakam na spracovanie odozvy" << endl;
	//blok sluzi len na vytvorenie falosneho spracovania a spravy zo strany serveru
	{
		struct sockaddr_in server2;
		struct sockaddr_in client2;
		begin_time = clock();
		char *kal_buf = (char*)malloc(30 * sizeof(char));
		int recv_len;
		if ((recv_len = recvfrom(server_socket, kal_buf, 30, 0, (struct sockaddr *) &client2, &slen)) == SOCKET_ERROR)//prijmem od klienta
		{
			printf("recvfrom() failed with error code : %d", WSAGetLastError());
			exit(EXIT_FAILURE);
		}
		checkCRC(kal_buf, 6);
		parse_first(filled_arr);
		snprintf(client_ip, 16, "%d.%d.%d.%d", (unsigned char)(kal_buf[0] & 0xff), (unsigned char)(kal_buf[1] & 0xff),
			(unsigned char)(kal_buf[2] & 0xff), (unsigned char)(kal_buf[3] & 0xff));
		port_klient = (kal_buf[4] << 8) + kal_buf[5];

		client2.sin_family = AF_INET;
		client2.sin_addr.s_addr = inet_addr(client_ip);
		client2.sin_port = htons(port_klient);

		if (sendto(sending_socket, "", 0, 0, (struct sockaddr *) &client2, sizeof(client2)) == SOCKET_ERROR)
		{
			printf("sendto() failed with error code : %d", WSAGetLastError());
			exit(EXIT_FAILURE);
		}
	}
	while (1) {

		memset(client_ip, '\0', 16);
		sprava.clear();
		if (accept_packets() == -1)
			continue;

		printf("Prijaty paket od %s:%d\n", inet_ntoa(client.sin_addr), ntohs(client.sin_port));
		printf("Sprava: %s\n", sprava.c_str());
	}
	return;
}

void send_keep_alive() {
	struct sockaddr pom_struct;
	int slen = sizeof(pom_struct);
	struct sockaddr_in server2;
	server2.sin_family = AF_INET;
	server2.sin_addr.s_addr = inet_addr(server_ip);
	server2.sin_port = htons(port_server);
	string info = "";
	info += (unsigned char)(clientIP[0] & 0xff);
	info += (unsigned char)(clientIP[1] & 0xff);
	info += (unsigned char)(clientIP[2] & 0xff);
	info += (unsigned char)(clientIP[3] & 0xff);
	info += (port_klient >> 8) & 0xff;
	info += (port_klient) & 0xff;

	std::uint32_t crc = CRC::Calculate(info.c_str(), info.length(), CRC::CRC_32());

	info += (crc >> 24) & 0xFF;
	info += (crc >> 16) & 0xFF;
	info += (crc >> 8) & 0xFF;
	info += crc & 0xFF;;


	clock_t start = clock();
	if (sendto(sending_socket, info.c_str(), info.size(), 0, (struct sockaddr *) &server2, sizeof(server2)) == SOCKET_ERROR)
	{
		printf("sendto() failed with error code : %d", WSAGetLastError());
		exit(EXIT_FAILURE);
	}

	if ((recvfrom(client_socket, buf, BUFLEN, 0, (struct sockaddr *) &pom_struct, &slen)) == SOCKET_ERROR)
	{
		printf("recvfrom() failed with error code : %d", WSAGetLastError());
		exit(EXIT_FAILURE);
	}
	clock_t end = clock();
	round_trip_time = end - start;
	round_trip_time *= 2;//---------------------------NASOBENIE RTT-----------------------------
	begin_listen = 1;
	while (1) {
		if (sendto(sending_socket, "\0\0k", 3, 0, (struct sockaddr *) &server2, sizeof(server2)) == SOCKET_ERROR)
		{
			printf("sendto() failed with error code : %d", WSAGetLastError());
			exit(EXIT_FAILURE);
		}
		Sleep(15000);
	}
}

void terminate() {
	begin_time = clock();
	while (float(akt_time - begin_time) / CLOCKS_PER_SEC <100) {
		Sleep(1000);
		akt_time = clock();
	}
	printf("\n--------------------------------------------------\n");
	printf("Keep alive neprislo 60 sekund, zatvaranie serveru\n");
	printf("\n--------------------------------------------------\n");
	exit(1);
}

int main()
{
	//-----------------------Deklaracia premennych------------------
	server_ip = (char*)malloc(16 * sizeof(char));
	client_ip = (char*)malloc(16 * sizeof(char));
	memset(server_ip, '\0', 16);
	memset(client_ip, '\0', 16);
	buf = (char*)malloc(BUFLEN * sizeof(char));
	int ip[4];
	//-----------------------SERVER/CLIENT--------------------------
	bool server_select = false, client_select = false;
	while (1) {
		cout << "Choose \n SERVER- s\n CLIENT- c\n";
		char answer;
		cin >> answer;
		switch (tolower(answer)) {
		case 'c':
			client_select = true;
			break;
		case 's':
			server_select = true;
			break;
		default:
			cout << "Nacitajte realnu moznost\n";
		}
		if (client_select || server_select)
			break;
	}
	memset((void *)&server, '\0', sizeof(struct sockaddr_in));
	memset((void *)&client, '\0', sizeof(struct sockaddr_in));

	//--------------Nacitanie ako klient----------------------------
	if (client_select) {
		char c;
		cout << "Default -d" << endl;
		scanf(" %c", &c);
		if (c == 'd') {
			fragment_size = 1;
			port_server = 12345;
			port_klient = 12346;
			snprintf(server_ip, 16, "%d.%d.%d.%d", 147, 175, 181, 210);
			clientIP[0] = 147;
			clientIP[1] = 175;
			clientIP[2] = 181;
			clientIP[3] = 196;
		}
		else {
			fragment_size = 61000;
			while (fragment_size > 1500) {
				cout << "Nacitajte velkost fragmentov, maximalna dlzka 1500\n";
				cin >> fragment_size;
			}

			cout << "Nacitajte IP adresu serveru\n";
			scanf("%d.%d.%d.%d", &ip[0], &ip[1], &ip[2], &ip[3]);
			while (ip[0] < 0 || ip[0]>255 || ip[1] < 0 || ip[1]>255 || ip[2] < 0 || ip[2]>255 || ip[3] < 0 || ip[3]>255) {
				cout << "IP adresa nie je validna, nacitajte IP adresu serveru\n";
				scanf("%d.%d.%d.%d", &ip[0], &ip[1], &ip[2], &ip[3]);
			}
			cout << "Validna IP\n";
			snprintf(server_ip, 16, "%d.%d.%d.%d", ip[0], ip[1], ip[2], ip[3]);

			cout << "Nacitajte IP adresu tejto masiny\n";
			scanf("%d.%d.%d.%d", &clientIP[0], &clientIP[1], &clientIP[2], &clientIP[3]);
			while (clientIP[0] < 0 || clientIP[0]>255 || clientIP[1] < 0 || clientIP[1]>255 ||
				clientIP[2] < 0 || clientIP[2]>255 || clientIP[3] < 0 || clientIP[3]>255) {
				cout << "IP adresa nie je validna, nacitajte IP adresu serveru\n";
				scanf("%d.%d.%d.%d", &clientIP[0], &clientIP[1], &clientIP[2], &clientIP[3]);
			}
			cout << "Validna IP\n";

			cout << "Nacitajte na akom porte pocuva server\n";
			cin >> port_server;
			cout << "Nacitajte na akom porte pocuva tato masina\n";
			cin >> port_klient;
		}

	}


	//--------------Nacitanie ako server----------------------------
	if (server_select) {
		cout << "Nacitajte cislo portu serveru\n";
		cin >> port_server;
	}


	//-------------------univerzalny init--------------------------
	/* Open windows connection, asi na pridelovanie connectionov? */
	WSADATA wsaData;
	int iResult = WSAStartup(MAKEWORD(2, 2), &wsaData);
	if (iResult != NO_ERROR) {
		wprintf(L"WSAStartup function failed with error: %d\n", iResult);
		return 1;
	}

	if ((sending_socket = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP)) == SOCKET_ERROR)
	{
		printf("socket() failed with error code : %d", WSAGetLastError());
		exit(EXIT_FAILURE);
	}

	//--------------Main loop server--------------------------------
	if (server_select) {
		/* Open a datagram socket */
		if ((server_socket = socket(AF_INET, SOCK_DGRAM, 0)) == INVALID_SOCKET) {
			fprintf(stderr, "Could not create socket.\n");
			WSACleanup();
			exit(0);
		}
		//nastavenie sameho seba
		server.sin_family = AF_INET;
		server.sin_addr.s_addr = INADDR_ANY;//inet_addr("127.0.0.1")
		server.sin_port = htons(port_server);

		if (::bind(server_socket, (SOCKADDR *)&server, sizeof(server)) == SOCKET_ERROR)//bind port_server na strukturu server
		{
			printf("Bind failed with error code : %d", WSAGetLastError());
			exit(EXIT_FAILURE);
		}
		std::thread first(listenServer);//main leep for server
		std::thread second(terminate);//if keep alive does not come in 1 minute, close server

		first.join();
		second.join();
	}
	//--------------Main loop client--------------------------------
	if (client_select) {
		if ((client_socket = socket(AF_INET, SOCK_DGRAM, 0)) == INVALID_SOCKET) {
			fprintf(stderr, "Could not create socket.\n");
			WSACleanup();
			exit(0);
		}
		//na tuto strukturu budem posielat
		server.sin_family = AF_INET;
		server.sin_addr.s_addr = inet_addr(server_ip);
		server.sin_port = htons(port_server);//1234

											 //na tejto strukture pocuvam
		client.sin_family = AF_INET;
		client.sin_addr.s_addr = INADDR_ANY;
		client.sin_port = htons(port_klient);//port klienta na ktorom pocuva 1235

		if (::bind(client_socket, (SOCKADDR *)&client, sizeof(client)) == SOCKET_ERROR)//bind port_klient na strukturu client
		{
			printf("Bind failed with error code : %d", WSAGetLastError());
			exit(EXIT_FAILURE);
		}

		std::thread first(listenClient);//vytvori sa a spusti thread
		std::thread second(writeClient);
		std::thread third(send_keep_alive);

		first.join();
		second.join();
		third.join();
	}
	return 0;
}