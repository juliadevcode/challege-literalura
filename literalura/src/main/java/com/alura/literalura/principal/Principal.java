package com.alura.literalura.principal;

import com.alura.literalura.model.Autor;
import com.alura.literalura.model.Livro;
import com.alura.literalura.repository.AutorRepository;
import com.alura.literalura.repository.LivroRepository;
import com.alura.literalura.service.GutendexService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

@Component
public class Principal {

    private final GutendexService gutendexService;
    private final AutorRepository autorRepository;
    private final LivroRepository livroRepository;

    public Principal(GutendexService gutendexService,
                     AutorRepository autorRepository,
                     LivroRepository livroRepository) {
        this.gutendexService = gutendexService;
        this.autorRepository = autorRepository;
        this.livroRepository = livroRepository;
    }

    public void executarMenu() {
        Scanner scanner = new Scanner(System.in);
        boolean rodando = true;

        System.out.println("=== Bem-vindo ao LiterAlura (console) ===");

        while (rodando) {
            System.out.println("\nEscolha uma opção:");
            System.out.println("1 - Buscar livro pelo título (usa API Gutendex e salva no banco)");
            System.out.println("2 - Listar livros registrados");
            System.out.println("3 - Listar nossos autores");
            System.out.println("4 - Listar autores vivos em determinado ano");
            System.out.println("5 - Listar livros por idioma");
            System.out.println("0 - Sair");
            System.out.print("> ");

            String opcao = scanner.nextLine().trim();

            switch (opcao) {
                case "1" -> buscarLivro(scanner);
                case "2" -> listarLivros();
                case "3" -> listarAutores();
                case "4" -> listarAutoresVivos(scanner);
                case "5" -> listarLivrosPorIdioma(scanner);
                case "0" -> {
                    rodando = false;
                    System.out.println("Saindo... tchau!");
                }
                default -> System.out.println("Opção inválida. Tente novamente.");
            }
        }

        scanner.close();
    }

    private void buscarLivro(Scanner scanner) {
        System.out.print("Digite o título a buscar: ");
        String titulo = scanner.nextLine().trim();
        Optional<JsonNode> resultado = gutendexService.buscarLivroPorTitulo(titulo);

        if (resultado.isPresent()) {
            JsonNode book = resultado.get();
            String title = book.path("title").asText();
            JsonNode authors = book.path("authors");
            String authorName = "Desconhecido";
            Integer birthYear = null;
            Integer deathYear = null;

            if (authors.isArray() && authors.size() > 0) {
                JsonNode a = authors.get(0);
                authorName = a.path("name").asText();
                if (a.has("birth_year") && !a.get("birth_year").isNull()) birthYear = a.get("birth_year").asInt();
                if (a.has("death_year") && !a.get("death_year").isNull()) deathYear = a.get("death_year").asInt();
            }

            String idioma = "unknown";
            JsonNode langs = book.path("languages");
            if (langs.isArray() && langs.size() > 0) idioma = langs.get(0).asText();

            int downloads = book.path("download_count").asInt(0);

            Autor autor = autorRepository.findByNome(authorName);
            if (autor == null) {
                autor = new Autor(authorName, birthYear, deathYear);
            }

            Livro livro = new Livro(title, idioma.toUpperCase(), downloads);
            autor.adicionarLivro(livro);

            autorRepository.save(autor);

            System.out.printf("%n=== Livro Salvo no Banco ===%n");
            System.out.printf("Título: %s%nAutor: %s%nAno Nascimento: %s%nAno Falecimento: %s%nIdioma: %s%nDownloads: %d%n%n",
                    title, authorName,
                    birthYear == null ? "?" : birthYear,
                    deathYear == null ? "?" : deathYear,
                    idioma.toUpperCase(), downloads);

        } else {
            System.out.println("Nenhum resultado encontrado na Gutendex para esse título.");
        }
    }

    private void listarLivros() {
        List<Livro> todos = livroRepository.findAll();
        if (todos.isEmpty()) {
            System.out.println("Nenhum livro cadastrado.");
        } else {
            System.out.println("\n=== Livros Cadastrados ===");
            for (Livro l : todos) {
                String autorNome = l.getAutor() != null ? l.getAutor().getNome() : "—";
                String anoNasc = l.getAutor() != null && l.getAutor().getAnoNascimento() != null
                        ? l.getAutor().getAnoNascimento().toString() : "?";
                String anoObito = l.getAutor() != null && l.getAutor().getAnoFalecimento() != null
                        ? l.getAutor().getAnoFalecimento().toString() : "?";
                System.out.printf("Título: %s%nAutor: %s (nasc: %s, obito: %s)%nIdioma: %s%nDownloads: %d%n%n",
                        l.getTitulo(), autorNome, anoNasc, anoObito, l.getIdioma(), l.getDownloads());
            }
        }
    }

    private void listarAutores() {
        List<Autor> autores = autorRepository.findAll();
        if (autores.isEmpty()) {
            System.out.println("Nenhum autor cadastrado.");
        } else {
            System.out.println("\n=== Autores Cadastrados ===");
            for (Autor a : autores) {
                System.out.printf("Nome: %s%nAno Nascimento: %s%nAno Falecimento: %s%nLivros: ",
                        a.getNome(),
                        a.getAnoNascimento() == null ? "?" : a.getAnoNascimento(),
                        a.getAnoFalecimento() == null ? "?" : a.getAnoFalecimento()
                );
                if (!a.getLivros().isEmpty()) {
                    a.getLivros().forEach(l -> System.out.print(l.getTitulo() + "; "));
                } else {
                    System.out.print("—");
                }
                System.out.println("\n");
            }
        }
    }

    private void listarAutoresVivos(Scanner scanner) {
        System.out.print("Informe o ano (ex: 1800): ");
        try {
            int ano = Integer.parseInt(scanner.nextLine().trim());
            List<Autor> vivos = autorRepository.findAutoresVivosEmAno(ano);
            if (vivos.isEmpty()) {
                System.out.println("Nenhum autor vivo nesse ano.");
            } else {
                System.out.println("\n=== Autores Vivos no Ano " + ano + " ===");
                vivos.forEach(a -> System.out.printf("Nome: %s | Nasc: %s | Obito: %s%n",
                        a.getNome(),
                        a.getAnoNascimento() == null ? "?" : a.getAnoNascimento(),
                        a.getAnoFalecimento() == null ? "?" : a.getAnoFalecimento()));
            }
        } catch (NumberFormatException e) {
            System.out.println("Ano inválido.");
        }
    }

    private void listarLivrosPorIdioma(Scanner scanner) {
        System.out.print("Informe a abreviação do idioma (PT, EN, FR, ES): ");
        String lang = scanner.nextLine().trim();
        if (lang.isEmpty()) {
            System.out.println("Idioma inválido.");
            return;
        }
        List<Livro> porIdioma = livroRepository.findByIdiomaIgnoreCase(lang);
        if (porIdioma.isEmpty()) {
            System.out.println("Nenhum livro encontrado no idioma informado.");
        } else {
            System.out.println("\n=== Livros no idioma " + lang.toUpperCase() + " ===");
            porIdioma.forEach(l -> {
                String autorNome = l.getAutor() != null ? l.getAutor().getNome() : "—";
                System.out.printf("Título: %s | Autor: %s | Downloads: %d%n",
                        l.getTitulo(), autorNome, l.getDownloads());
            });
        }
    }
}
