#include <iostream>
#include <string>
#include <winsock2.h>
#include <ws2tcpip.h>

#define SERVER_IP "127.0.0.1"
#define PORT 9999

int main() {
    WSADATA wsaData;
    if (WSAStartup(MAKEWORD(2, 2), &wsaData) != 0) {
        std::cerr << "WSAStartup failed.\n";
        return 1;
    }

    SOCKET sock = socket(AF_INET, SOCK_STREAM, 0);
    if (sock == INVALID_SOCKET) {
        std::cerr << "Socket creation failed.\n";
        WSACleanup();
        return 1;
    }

    sockaddr_in server{};
    server.sin_family = AF_INET;
    server.sin_port = htons(PORT);
    inet_pton(AF_INET, SERVER_IP, &server.sin_addr);

    std::cout << "Connecting to Java Server (" << SERVER_IP << ":" << PORT << ")..." << std::endl;

    if (connect(sock, (sockaddr*)&server, sizeof(server)) != 0) {
        std::cerr << "Connection failed! Check IP and Firewall." << std::endl;
        closesocket(sock);
        WSACleanup();
        return 1;
    }

    char buffer[4096];

    int bytesReceived = recv(sock, buffer, 4096, 0);
    if (bytesReceived > 0) {
        buffer[bytesReceived] = '\0';
        std::cout << "\n[SERVER]: " << buffer << std::endl;
    }

    while (true) {
        std::string query;
        std::cout << "\n[C++] Enter query (or 'quit'): ";
        std::getline(std::cin, query);

        if (query.empty()) continue;
        if (query == "quit" || query == "exit") {
            std::string exitCmd = "EXIT\n";
            send(sock, exitCmd.c_str(), exitCmd.length(), 0);
            break;
        }

        std::string request = "SEARCH " + query + "\n";

        int sendResult = send(sock, request.c_str(), request.length(), 0);
        if (sendResult == SOCKET_ERROR) {
            std::cerr << "Send failed." << std::endl;
            break;
        }

        bytesReceived = recv(sock, buffer, 4096, 0);
        if (bytesReceived > 0) {
            buffer[bytesReceived] = '\0';

            if (std::string(buffer).find("NOT_FOUND") == 0) {
                std::cout << " -> Not found." << std::endl;
            }
            else if (std::string(buffer).find("FOUND") == 0) {
                std::cout << " -> [SUCCESS] Data received from server." << std::endl;
                std::cout << "RAW RESPONSE: " << buffer << std::endl;
            }
            else {
                std::cout << " -> " << buffer << std::endl;
            }
        } else {
            std::cout << "Server disconnected." << std::endl;
            break;
        }
    }

    closesocket(sock);
    WSACleanup();
    std::cout << "Client closed." << std::endl;

    system("pause");
    return 0;
}
